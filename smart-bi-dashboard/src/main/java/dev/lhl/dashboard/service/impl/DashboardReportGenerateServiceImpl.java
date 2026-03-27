package dev.lhl.dashboard.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import dev.lhl.dashboard.domain.ChartCard;
import dev.lhl.dashboard.domain.report.ReportGenerateProgressCallback;
import dev.lhl.dashboard.domain.report.ReportGenerateRequest;
import dev.lhl.dashboard.domain.report.ReportGenerateResult;
import dev.lhl.dashboard.service.IChartCardService;
import dev.lhl.dashboard.service.IDashboardReportGenerateService;
import dev.lhl.metadata.domain.TableMetadata;
import dev.lhl.metadata.service.IMetadataService;
import dev.lhl.query.domain.QueryRecord;
import dev.lhl.query.service.INl2SqlService;
import dev.lhl.query.service.IQueryExecutionService;
import dev.lhl.query.service.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 看板报表生成：意图规划 LLM → 逐项 NL2SQL → 本服务执行 SQL → 生成图表卡片（ChartCard）。
 * 使用本服务 ETL 数据源执行查询，不创建数据源卡片，避免在外部数据源上执行导致表不存在。
 */
@Service
public class DashboardReportGenerateServiceImpl implements IDashboardReportGenerateService {

    private static final Logger log = LoggerFactory.getLogger(DashboardReportGenerateServiceImpl.class);

    private static final String PLANNING_PROMPT_TEMPLATE = """
        你是一个数据大屏规划助手。根据用户的自然语言描述，输出该大屏应包含的报表元素列表。
        
        可用表（仅作参考，SQL 由后续步骤生成）：\n%s
        
        请严格输出一个 JSON 数组，每项包含：
        - title: 报表标题（简短中文）
        - chartType: 图表类型，只能从 [bar, line, pie, table, kpi, groupedBar] 中选一
        - queryDescription: 用于生成该报表 SQL 的自然语言查询描述（一句完整问句，如「各区域销售额汇总」）
        
        示例：[{"title":"销售趋势","chartType":"line","queryDescription":"近12个月每月销售额"}]
        不要输出 markdown 或其它说明，仅输出 JSON 数组。
        
        用户描述：%s
        """;

    @Autowired(required = false)
    private LlmService llmService;

    @Autowired(required = false)
    private INl2SqlService nl2SqlService;

    @Autowired(required = false)
    private IMetadataService metadataService;

    @Autowired(required = false)
    private IQueryExecutionService queryExecutionService;

    @Autowired(required = false)
    private IChartCardService chartCardService;

    private static final int CARD_WIDTH = 480;
    private static final int CARD_HEIGHT = 320;
    private static final int COLS = 2;
    private static final int GAP = 24;
    private static final int START_X = 40;
    private static final int START_Y = 40;

    @Override
    public ReportGenerateResult generateReport(ReportGenerateRequest request, Long userId) {
        return generateReportInternal(request, userId, null);
    }

    @Override
    public ReportGenerateResult generateReport(ReportGenerateRequest request, Long userId, ReportGenerateProgressCallback callback) {
        return generateReportInternal(request, userId, callback);
    }

    private ReportGenerateResult generateReportInternal(ReportGenerateRequest request, Long userId, ReportGenerateProgressCallback callback) {
        if (request == null || request.getPrompt() == null || request.getPrompt().isBlank()) {
            if (callback != null) callback.onError("INVALID_PROMPT", "请填写大屏描述（prompt）");
            return ReportGenerateResult.failure("INVALID_PROMPT", "请填写大屏描述（prompt）");
        }
        log.info("报表生成: prompt={}, dashboardId={}, userId={}",
            request.getPrompt(), request.getDashboardId(), userId);

        if (callback != null) callback.onMessage("正在规划报表…");
        String tableListText = buildTableListForPrompt(request.getRestrictToTableIds());

        // 1. 意图 + 报表规划 LLM
        List<PlannedItem> planned;
        try {
            planned = callPlanningLlm(request.getPrompt(), tableListText);
        } catch (Exception e) {
            log.warn("报表规划 LLM 失败", e);
            String msg = "规划失败，请重试或简化描述：" + (e.getMessage() != null ? e.getMessage() : "未知错误");
            if (callback != null) callback.onError("PLANNING_FAILED", msg);
            return ReportGenerateResult.failure("PLANNING_FAILED", msg);
        }
        if (planned == null || planned.isEmpty()) {
            if (callback != null) callback.onError("NO_PLAN", "未能解析出报表元素，请换一种方式描述");
            return ReportGenerateResult.failure("NO_PLAN", "未能解析出报表元素，请换一种方式描述");
        }
        if (callback != null) callback.onMessage("规划完成，共 " + planned.size() + " 项，开始生成…");

        // 2. 逐项 NL2SQL → 本服务执行 SQL → 生成图表卡片（ChartCard）
        List<Map<String, Object>> cards = new ArrayList<>();
        List<Map<String, Object>> layout = buildLayout(planned.size());
        for (int i = 0; i < planned.size(); i++) {
            PlannedItem item = planned.get(i);
            if (callback != null) callback.onMessage("正在生成第 " + (i + 1) + "/" + planned.size() + " 张：\u200b" + item.title + "\u200b…");
            Map<String, Object> card = buildChartCardForItem(item, userId, i);
            if (card != null) {
                Map<String, Object> layoutItem = i < layout.size() ? layout.get(i) : new LinkedHashMap<>();
                card.put("positionX", layoutItem.get("x"));
                card.put("positionY", layoutItem.get("y"));
                card.put("width", layoutItem.getOrDefault("w", CARD_WIDTH));
                card.put("height", layoutItem.getOrDefault("h", CARD_HEIGHT));
                cards.add(card);
                if (callback != null) {
                    callback.onCardReady(i, card, layoutItem);
                    callback.onMessage("第 " + (i + 1) + " 张已生成：\u200b" + item.title + "\u200b，已加入画布");
                }
            } else if (callback != null) {
                callback.onMessage("第 " + (i + 1) + " 张跳过（生成失败）");
            }
        }

        if (cards.isEmpty()) {
            if (callback != null) callback.onError("NO_CARDS", "未能成功生成任何报表卡片，请检查描述或数据源配置");
            return ReportGenerateResult.failure("NO_CARDS", "未能成功生成任何报表卡片，请检查描述或数据源配置");
        }

        if (callback != null) callback.onComplete();
        return ReportGenerateResult.success(cards, layout);
    }

    private String buildTableListForPrompt(List<Long> restrictToTableIds) {
        if (metadataService == null) return "（未提供表列表）";
        try {
            List<TableMetadata> tables = metadataService.selectTableMetadataListForNl2Sql(restrictToTableIds);
            if (tables == null || tables.isEmpty()) return "（暂无可用表）";
            return tables.stream()
                .map(t -> "- " + t.getTableName() + (t.getTableComment() != null && !t.getTableComment().isEmpty() ? "（" + t.getTableComment() + "）" : ""))
                .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.debug("获取表列表失败", e);
            return "（未提供表列表）";
        }
    }

    private List<PlannedItem> callPlanningLlm(String userPrompt, String tableListText) {
        if (llmService == null || !llmService.isAvailable()) {
            throw new RuntimeException("大模型未配置或不可用");
        }
        String promptText = String.format(PLANNING_PROMPT_TEMPLATE, tableListText, userPrompt);
        Prompt prompt = new Prompt(new UserMessage(promptText));
        String raw = llmService.callPrompt(prompt);
        if (raw == null || raw.isBlank()) {
            throw new RuntimeException("规划 LLM 未返回内容");
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf("[");
            int end = trimmed.lastIndexOf("]");
            if (start >= 0 && end > start) trimmed = trimmed.substring(start, end + 1);
        }
        JSONArray arr = JSON.parseArray(trimmed);
        if (arr == null || arr.isEmpty()) return Collections.emptyList();
        List<PlannedItem> list = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String title = o.getString("title");
            String chartType = o.getString("chartType");
            String queryDescription = o.getString("queryDescription");
            if (title == null) title = "报表" + (i + 1);
            if (chartType == null) chartType = "table";
            if (queryDescription == null || queryDescription.isBlank()) queryDescription = title;
            list.add(new PlannedItem(title, chartType, queryDescription));
        }
        return list;
    }

    /**
     * 单条规划项：NL2SQL → 本服务执行 SQL（ETL 数据源）→ 生成 ChartCard 并返回画布卡片结构
     */
    private Map<String, Object> buildChartCardForItem(PlannedItem item, Long userId, int index) {
        if (nl2SqlService == null || queryExecutionService == null || chartCardService == null) {
            log.warn("报表生成依赖服务未注入: nl2Sql/queryExecution/chartCard");
            return null;
        }
        try {
            QueryRecord record = nl2SqlService.generateSQL(item.queryDescription, userId, null);
            if (record == null || record.getGeneratedSql() == null || record.getGeneratedSql().isBlank()) {
                return null;
            }
            String sqlToRun = record.getExecutedSql() != null && !record.getExecutedSql().isBlank()
                ? record.getExecutedSql() : record.getGeneratedSql();

            QueryRecord execRecord = new QueryRecord();
            execRecord.setExecutedSql(sqlToRun);
            execRecord.setUserId(userId);
            IQueryExecutionService.QueryResult result = queryExecutionService.executeQuery(execRecord, userId);
            if (!result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
                log.debug("执行 SQL 无数据或失败 for item: {} - {}", item.title, result.getErrorMessage());
                return null;
            }

            List<Map<String, Object>> data = result.getData();
            List<String> columns = new ArrayList<>(data.get(0).keySet());
            String chartTypeCode = normalizeChartType(item.chartType);

            // 转为前端可解析的纯 JSON 结构，避免 BigDecimal/Timestamp 等序列化后前端解析异常
            List<Map<String, Object>> dataJsonFriendly = toJsonFriendlyRows(data);

            // 前端 CardRenderer 期望 chartConfig 为 { type, columns, data }，表格/KPI 直接取 data 渲染，柱/线/饼再调接口生成 ECharts option
            Map<String, Object> chartConfigForStorage = new LinkedHashMap<>();
            chartConfigForStorage.put("type", chartTypeCode);
            chartConfigForStorage.put("columns", columns);
            chartConfigForStorage.put("data", dataJsonFriendly);

            ChartCard chartCard = new ChartCard();
            chartCard.setName(item.title);
            chartCard.setChartType(chartTypeCode);
            chartCard.setChartConfig(JSON.toJSONString(chartConfigForStorage));
            chartCard.setSql(sqlToRun);
            chartCardService.insertChartCard(chartCard);

            Map<String, Object> card = new LinkedHashMap<>();
            card.put("dashboardCardId", null);
            card.put("cardId", chartCard.getId());
            card.put("componentType", "chart");
            card.put("cardName", item.title);
            card.put("chartType", chartCard.getChartType());
            card.put("chartConfig", chartCard.getChartConfig());
            card.put("positionX", 0);
            card.put("positionY", 0);
            card.put("width", CARD_WIDTH);
            card.put("height", CARD_HEIGHT);
            card.put("sortOrder", index + 1);
            return card;
        } catch (Exception e) {
            log.debug("生成图表卡片失败 for item: {} - {}", item.title, e.getMessage());
            return null;
        }
    }

    private static String normalizeChartType(String s) {
        if (s == null) return "table";
        String t = s.trim().toLowerCase();
        Set<String> allowed = Set.of("bar", "line", "pie", "table", "kpi", "groupedbar", "grouped_bar");
        if (allowed.contains(t) || allowed.contains(t.replace("_", ""))) {
            return "grouped_bar".equals(t) || "groupedbar".equals(t) ? "groupedBar" : t;
        }
        return "table";
    }

    /** 将查询结果每行转为仅含 Number/String/Boolean/null 的 Map，保证 JSON 序列化后前端能正确解析 */
    private static List<Map<String, Object>> toJsonFriendlyRows(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) return data;
        List<Map<String, Object>> out = new ArrayList<>(data.size());
        for (Map<String, Object> row : data) {
            Map<String, Object> clean = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : row.entrySet()) {
                Object v = e.getValue();
                clean.put(e.getKey(), toJsonFriendlyValue(v));
            }
            out.add(clean);
        }
        return out;
    }

    private static Object toJsonFriendlyValue(Object v) {
        if (v == null) return null;
        if (v instanceof Number || v instanceof Boolean || v instanceof String) return v;
        if (v instanceof java.util.Date) return ((java.util.Date) v).getTime();
        if (v instanceof java.time.temporal.TemporalAccessor) return v.toString();
        if (v instanceof byte[]) return new String((byte[]) v, java.nio.charset.StandardCharsets.UTF_8);
        return String.valueOf(v);
    }

    private List<Map<String, Object>> buildLayout(int cardCount) {
        List<Map<String, Object>> layout = new ArrayList<>();
        for (int i = 0; i < cardCount; i++) {
            int row = i / COLS;
            int col = i % COLS;
            int x = START_X + col * (CARD_WIDTH + GAP);
            int y = START_Y + row * (CARD_HEIGHT + GAP);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("cardKey", i);
            item.put("x", x);
            item.put("y", y);
            item.put("w", CARD_WIDTH);
            item.put("h", CARD_HEIGHT);
            layout.add(item);
        }
        return layout;
    }

    private static class PlannedItem {
        final String title;
        final String chartType;
        final String queryDescription;

        PlannedItem(String title, String chartType, String queryDescription) {
            this.title = title;
            this.chartType = chartType;
            this.queryDescription = queryDescription;
        }
    }
}
