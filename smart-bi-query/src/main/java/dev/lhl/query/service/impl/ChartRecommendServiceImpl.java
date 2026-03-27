package dev.lhl.query.service.impl;

import dev.lhl.query.service.IChartRecommendService;
import dev.lhl.query.service.LlmService;
import dev.lhl.query.service.PromptTemplateService;
import dev.lhl.common.utils.ChartRecommendUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 图表推荐服务实现
 * 根据查询结果特征推荐合适的图表类型
 * 
 * @author smart-bi
 */
@Service
public class ChartRecommendServiceImpl implements IChartRecommendService
{
    private static final Logger log = LoggerFactory.getLogger(ChartRecommendServiceImpl.class);

    @Autowired(required = false)
    private LlmService llmService;

    @Autowired(required = false)
    private PromptTemplateService promptTemplateService;

    @Override
    public ChartType recommendChartType(List<String> columns, List<Map<String, Object>> data)
    {
        return recommendChartType(columns, data, null, null);
    }

    @Override
    public ChartType recommendChartType(List<String> columns, List<Map<String, Object>> data, String question, String sql)
    {
        if (columns == null || columns.isEmpty() || data == null || data.isEmpty())
        {
            log.debug("数据为空，推荐指标卡");
            return ChartType.KPI;
        }

        // 优先尝试 LLM 推荐（需配置大模型且存在提示词服务）
        if (llmService != null && llmService.isAvailable() && promptTemplateService != null)
        {
            ChartType llmType = recommendChartTypeByLlm(columns, data, question, sql);
            if (llmType != null)
            {
                log.debug("LLM 推荐图表类型: {}", llmType.getCode());
                return llmType;
            }
        }

        return recommendChartTypeByRules(columns, data);
    }

    @Override
    public ChartRecommendation recommendChartTypeWithConfidence(List<String> columns, List<Map<String, Object>> data, String question, String sql)
    {
        if (columns == null || columns.isEmpty() || data == null || data.isEmpty())
        {
            return new ChartRecommendation(ChartType.KPI, 0.5, List.of(ChartType.TABLE));
        }
        if (llmService != null && llmService.isAvailable() && promptTemplateService != null)
        {
            ChartRecommendation rec = recommendWithConfidenceByLlm(columns, data, question, sql);
            if (rec != null) return rec;
        }
        ChartType primary = recommendChartTypeByRules(columns, data);
        return new ChartRecommendation(primary, 0.6, List.of());
    }

    private ChartRecommendation recommendWithConfidenceByLlm(List<String> columns, List<Map<String, Object>> data, String question, String sql)
    {
        try
        {
            String sampleJson = buildSampleRowsJson(data, 5);
            var prompt = promptTemplateService.createChartRecommendPrompt(question, sql, columns, data.size(), sampleJson);
            String raw = llmService.callPrompt(prompt);
            if (raw == null || raw.isEmpty()) return null;
            var obj = parseChartRecommendJson(raw);
            if (obj == null) return null;
            ChartType primary = parseChartType(obj.chartType);
            if (primary == null) primary = ChartType.TABLE;
            List<ChartType> alts = new ArrayList<>();
            if (obj.alternatives != null)
                for (String a : obj.alternatives)
                {
                    ChartType t = parseChartType(a);
                    if (t != null && !primary.equals(t)) alts.add(t);
                }
            return new ChartRecommendation(primary, Math.max(0, Math.min(1, obj.confidence)), alts);
        }
        catch (Exception e)
        {
            log.debug("LLM 图表推荐（含置信度）失败: {}", e.getMessage());
            return null;
        }
    }

    private static ChartRecommendJson parseChartRecommendJson(String raw)
    {
        try
        {
            String s = raw.trim();
            if (s.startsWith("```")) { int i = s.indexOf("{"); int j = s.lastIndexOf("}"); if (i >= 0 && j > i) s = s.substring(i, j + 1); }
            var obj = com.alibaba.fastjson2.JSON.parseObject(s);
            if (obj == null || !obj.containsKey("chartType")) return null;
            String ct = obj.getString("chartType");
            double conf = 0.7;
            if (obj.containsKey("confidence")) { Object c = obj.get("confidence"); if (c instanceof Number) conf = ((Number) c).doubleValue(); }
            List<String> alts = new ArrayList<>();
            if (obj.containsKey("alternatives") && obj.get("alternatives") instanceof List)
            {
                for (Object a : (List<?>) obj.get("alternatives"))
                    if (a != null) alts.add(String.valueOf(a));
            }
            return new ChartRecommendJson(ct, conf, alts);
        }
        catch (Exception e) { return null; }
    }

    private static record ChartRecommendJson(String chartType, double confidence, List<String> alternatives) {}

    /**
     * 使用 LLM 推荐图表类型，失败返回 null（由调用方回退到规则）
     */
    private ChartType recommendChartTypeByLlm(List<String> columns, List<Map<String, Object>> data, String question, String sql)
    {
        try
        {
            String sampleJson = buildSampleRowsJson(data, 5);
            var prompt = promptTemplateService.createChartRecommendPrompt(
                question, sql, columns, data.size(), sampleJson);
            String raw = llmService.callPrompt(prompt);
            if (raw == null || raw.isEmpty())
            {
                return null;
            }
            String chartTypeCode = parseChartTypeFromLlmResponse(raw);
            if (chartTypeCode == null)
            {
                return null;
            }
            ChartType t = parseChartType(chartTypeCode);
            return t != null ? t : null;
        }
        catch (Exception e)
        {
            log.debug("LLM 图表推荐失败，将使用规则推荐: {}", e.getMessage());
            return null;
        }
    }

    private static String buildSampleRowsJson(List<Map<String, Object>> data, int maxRows)
    {
        if (data == null || data.isEmpty())
        {
            return "[]";
        }
        int end = Math.min(maxRows, data.size());
        List<Map<String, Object>> sub = data.subList(0, end);
        return com.alibaba.fastjson2.JSON.toJSONString(sub);
    }

    private static final java.util.regex.Pattern JSON_BLOCK = java.util.regex.Pattern.compile("\\{[\\s\\S]*?\"chartType\"\\s*:\\s*\"([^\"]+)\"[\\s\\S]*?\\}");

    private static String parseChartTypeFromLlmResponse(String raw)
    {
        if (raw == null)
        {
            return null;
        }
        // 尝试直接解析整段为 JSON
        try
        {
            var obj = com.alibaba.fastjson2.JSON.parseObject(raw);
            if (obj != null && obj.containsKey("chartType"))
            {
                return obj.getString("chartType");
            }
        }
        catch (Exception ignored) { }
        // 尝试从 ```json ... ``` 中提取
        int start = raw.indexOf("```");
        if (start >= 0)
        {
            start = raw.indexOf("{", start);
            if (start >= 0)
            {
                int end = raw.indexOf("}", start);
                if (end > start)
                {
                    try
                    {
                        var obj = com.alibaba.fastjson2.JSON.parseObject(raw.substring(start, end + 1));
                        if (obj != null && obj.containsKey("chartType"))
                        {
                            return obj.getString("chartType");
                        }
                    }
                    catch (Exception ignored) { }
                }
            }
        }
        // 正则兜底
        java.util.regex.Matcher m = JSON_BLOCK.matcher(raw);
        return m.find() ? m.group(1) : null;
    }

    private static ChartType parseChartType(String code)
    {
        if (code == null || code.isEmpty())
        {
            return null;
        }
        String c = code.trim().toLowerCase();
        for (ChartType t : ChartType.values())
        {
            if (t.getCode().equalsIgnoreCase(c))
            {
                return t;
            }
        }
        return null;
    }

    /**
     * 规则推荐（原逻辑）
     */
    private ChartType recommendChartTypeByRules(List<String> columns, List<Map<String, Object>> data)
    {
        try
        {
            // 分析数据特征
            int dimensionCount = 0;
            int measureCount = 0;
            boolean hasTimeDimension = false;
            int enumValueCount = 0;
            String firstDimension = null;
            
            for (String column : columns)
            {
                // 列名像维度（*_id、id、name、code 等）时优先计为维度，避免 customer_id 被当成度量导致推荐成 KPI
                if (isLikelyDimensionColumn(column))
                {
                    dimensionCount++;
                    if (firstDimension == null)
                    {
                        firstDimension = column;
                    }
                    if (isTimeColumn(column))
                    {
                        hasTimeDimension = true;
                    }
                }
                else if (isMeasureColumn(column, data))
                {
                    measureCount++;
                }
                else
                {
                    dimensionCount++;
                    if (firstDimension == null)
                    {
                        firstDimension = column;
                    }
                    if (isTimeColumn(column))
                    {
                        hasTimeDimension = true;
                    }
                }
            }
            
            // 计算枚举值数量（仅当有维度时）
            if (firstDimension != null)
            {
                enumValueCount = countEnumValues(data, firstDimension);
            }
            
            log.debug("数据特征分析: dimensionCount={}, measureCount={}, hasTimeDimension={}, enumValueCount={}",
                dimensionCount, measureCount, hasTimeDimension, enumValueCount);
            
            // 使用工具类推荐图表类型
            ChartRecommendUtils.ChartType recommendedType = ChartRecommendUtils.recommendChartType(
                columns, data, dimensionCount, measureCount, hasTimeDimension, enumValueCount
            );
            return convertToServiceType(recommendedType);
        }
        catch (Exception e)
        {
            log.error("图表推荐失败", e);
            return ChartType.TABLE;
        }
    }
    
    /**
     * 按列名判断是否更像维度（id、*_id、name、code、type 等），用于避免把 customer_id 等判成度量
     */
    private static boolean isLikelyDimensionColumn(String columnName)
    {
        if (columnName == null || columnName.isEmpty()) return false;
        String lower = columnName.toLowerCase();
        return lower.endsWith("_id") || "id".equals(lower)
            || lower.contains("name") || lower.contains("code") || lower.contains("type")
            || lower.contains("category") || lower.contains("level") || lower.contains("status");
    }

    /**
     * 判断是否为度量列（数值类型）
     */
    private boolean isMeasureColumn(String column, List<Map<String, Object>> data)
    {
        if (data.isEmpty())
        {
            return false;
        }
        
        Object firstValue = data.get(0).get(column);
        if (firstValue == null)
        {
            return false;
        }
        
        // 判断是否为数值类型
        return firstValue instanceof Number;
    }
    
    /**
     * 判断是否为时间列
     */
    private boolean isTimeColumn(String column)
    {
        String lowerColumn = column.toLowerCase();
        return lowerColumn.contains("date") || 
               lowerColumn.contains("time") || 
               lowerColumn.contains("年") || 
               lowerColumn.contains("月") ||
               lowerColumn.contains("日");
    }
    
    /**
     * 计算枚举值数量
     */
    private int countEnumValues(List<Map<String, Object>> data, String column)
    {
        Set<Object> uniqueValues = new HashSet<>();
        for (Map<String, Object> row : data)
        {
            Object value = row.get(column);
            if (value != null)
            {
                uniqueValues.add(value);
            }
        }
        return uniqueValues.size();
    }
    
    /**
     * 转换工具类枚举到服务接口枚举
     */
    private ChartType convertToServiceType(ChartRecommendUtils.ChartType utilsType)
    {
        switch (utilsType)
        {
            case BAR:
                return ChartType.BAR;
            case LINE:
                return ChartType.LINE;
            case PIE:
                return ChartType.PIE;
            case GROUPED_BAR:
                return ChartType.GROUPED_BAR;
            case KPI:
                return ChartType.KPI;
            case TABLE:
                return ChartType.TABLE;
            default:
                return ChartType.TABLE;
        }
    }
}
