package dev.lhl.web.service;

import dev.lhl.metadata.domain.TableMetadata;
import dev.lhl.query.domain.LlmAudit;
import dev.lhl.query.service.ILlmAuditService;
import dev.lhl.system.domain.SysConfig;
import dev.lhl.system.service.ISysConfigService;
import dev.lhl.web.domain.vo.TableMetadataVO;
import dev.lhl.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 表列表与问题表统计（聚合审计）
 *
 * @author smart-bi
 */
@Service
public class MetadataTableListService
{
    private static final String CONFIG_WINDOW_DAYS = "nl2sql.problemTable.windowDays";
    private static final String CONFIG_ERROR_THRESHOLD = "nl2sql.problemTable.errorCountThreshold";
    private static final int DEFAULT_WINDOW_DAYS = 30;
    private static final int DEFAULT_ERROR_THRESHOLD = 3;
    private static final int TOP_ERROR_CATEGORIES = 5;

    @Autowired(required = false)
    private ILlmAuditService llmAuditService;
    @Autowired(required = false)
    private ISysConfigService sysConfigService;

    /**
     * 获取问题表统计时间窗口（天）
     */
    public int getProblemTableWindowDays()
    {
        if (sysConfigService == null) return DEFAULT_WINDOW_DAYS;
        String v = sysConfigService.selectConfigByKey(CONFIG_WINDOW_DAYS);
        if (StringUtils.isEmpty(v)) return DEFAULT_WINDOW_DAYS;
        try { return Math.max(1, Integer.parseInt(v.trim())); } catch (NumberFormatException e) { return DEFAULT_WINDOW_DAYS; }
    }

    /**
     * 获取问题表错误次数阈值（≥此值则高亮）
     */
    public int getProblemTableErrorThreshold()
    {
        if (sysConfigService == null) return DEFAULT_ERROR_THRESHOLD;
        String v = sysConfigService.selectConfigByKey(CONFIG_ERROR_THRESHOLD);
        if (StringUtils.isEmpty(v)) return DEFAULT_ERROR_THRESHOLD;
        try { return Math.max(0, Integer.parseInt(v.trim())); } catch (NumberFormatException e) { return DEFAULT_ERROR_THRESHOLD; }
    }

    /**
     * 更新问题表高亮配置（若依系统参数）
     * @param windowDays 统计时间窗口（天），1–365；null 表示不更新
     * @param errorCountThreshold 错误次数阈值，≥0；null 表示不更新
     */
    public void updateProblemTableConfig(Integer windowDays, Integer errorCountThreshold)
    {
        if (sysConfigService == null) return;
        if (windowDays != null)
        {
            int v = Math.max(1, Math.min(365, windowDays));
            SysConfig q = new SysConfig();
            q.setConfigKey(CONFIG_WINDOW_DAYS);
            List<SysConfig> list = sysConfigService.selectConfigList(q);
            if (!list.isEmpty())
            {
                SysConfig c = list.get(0);
                c.setConfigValue(String.valueOf(v));
                sysConfigService.updateConfig(c);
            }
        }
        if (errorCountThreshold != null)
        {
            int v = Math.max(0, errorCountThreshold);
            SysConfig q = new SysConfig();
            q.setConfigKey(CONFIG_ERROR_THRESHOLD);
            List<SysConfig> list = sysConfigService.selectConfigList(q);
            if (!list.isEmpty())
            {
                SysConfig c = list.get(0);
                c.setConfigValue(String.valueOf(v));
                sysConfigService.updateConfig(c);
            }
        }
    }

    /**
     * 将表元数据列表填充问题表统计，返回 VO 列表
     */
    public List<TableMetadataVO> enrichWithAudit(List<TableMetadata> tables)
    {
        if (tables == null) return Collections.emptyList();
        int windowDays = getProblemTableWindowDays();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, -windowDays);
        Date startTime = cal.getTime();

        List<LlmAudit> errors = (llmAuditService != null) ? llmAuditService.listErrorsSince(startTime) : Collections.emptyList();
        Map<String, TableAuditStats> statsMap = buildTableAuditStats(errors);

        List<TableMetadataVO> result = new ArrayList<>(tables.size());
        for (TableMetadata t : tables)
        {
            TableMetadataVO vo = toVO(t);
            if (t.getTableName() != null)
            {
                TableAuditStats stats = statsMap.get(t.getTableName().trim());
                if (stats != null)
                {
                    vo.setErrorCount(stats.count);
                    vo.setLastErrorTime(stats.lastErrorTime);
                    vo.setLastErrorCategories(stats.getTopCategories(TOP_ERROR_CATEGORIES));
                }
            }
            if (vo.getErrorCount() == null) vo.setErrorCount(0);
            result.add(vo);
        }
        return result;
    }

    private Map<String, TableAuditStats> buildTableAuditStats(List<LlmAudit> errors)
    {
        Map<String, TableAuditStats> map = new HashMap<>();
        for (LlmAudit a : errors)
        {
            String recalled = a.getRecalledTables();
            Set<String> tableNames = parseTableNamesFromRecalled(recalled);
            Date createTime = a.getCreateTime();
            String category = StringUtils.isEmpty(a.getErrorCategory()) ? "OTHER" : a.getErrorCategory();
            for (String tableName : tableNames)
            {
                if (StringUtils.isEmpty(tableName)) continue;
                tableName = tableName.trim();
                map.computeIfAbsent(tableName, k -> new TableAuditStats()).add(createTime, category);
            }
        }
        return map;
    }

    /** recalled_tables 格式为逗号分隔表名 */
    private Set<String> parseTableNamesFromRecalled(String recalled)
    {
        Set<String> set = new HashSet<>();
        if (StringUtils.isEmpty(recalled)) return set;
        for (String s : recalled.split(","))
        {
            String t = s.trim();
            if (!t.isEmpty()) set.add(t);
        }
        return set;
    }

    private TableMetadataVO toVO(TableMetadata t)
    {
        TableMetadataVO vo = new TableMetadataVO();
        vo.setId(t.getId());
        vo.setTableName(t.getTableName());
        vo.setTableComment(t.getTableComment());
        vo.setBusinessDescription(t.getBusinessDescription());
        vo.setDomainId(t.getDomainId());
        vo.setTableUsage(t.getTableUsage());
        vo.setNl2sqlVisibilityLevel(t.getNl2sqlVisibilityLevel());
        vo.setGrainDesc(t.getGrainDesc());
        vo.setUpdateTime(t.getUpdateTime());
        return vo;
    }

    private static class TableAuditStats
    {
        int count;
        Date lastErrorTime;
        Map<String, Integer> categoryCounts = new HashMap<>();

        void add(Date time, String category)
        {
            count++;
            if (lastErrorTime == null || (time != null && time.after(lastErrorTime)))
                lastErrorTime = time;
            categoryCounts.merge(category, 1, Integer::sum);
        }

        List<String> getTopCategories(int n)
        {
            return categoryCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(n)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }
    }
}
