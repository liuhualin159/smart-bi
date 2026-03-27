package dev.lhl.query.service.impl;

import dev.lhl.common.utils.DateUtils;
import dev.lhl.common.utils.DesensitizeUtils;
import dev.lhl.common.utils.StringUtils;
import dev.lhl.query.domain.LlmAudit;
import dev.lhl.query.domain.dto.TableErrorSummary;
import dev.lhl.query.mapper.LlmAuditMapper;
import dev.lhl.query.service.ILlmAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM请求审计服务实现
 * 负责记录LLM请求和响应，包括PII脱敏
 * 
 * @author smart-bi
 */
@Service
public class LlmAuditServiceImpl implements ILlmAuditService
{
    private static final Logger log = LoggerFactory.getLogger(LlmAuditServiceImpl.class);
    
    // 手机号正则表达式
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    
    // 身份证号正则表达式（18位）
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\d{17}[\\dXx]");
    private static final Pattern TABLE_IN_SQL_PATTERN = Pattern.compile("(?:FROM|JOIN)\\s+[`]?(\\w+)[`]?", Pattern.CASE_INSENSITIVE);
    
    @Autowired(required = false)
    private LlmAuditMapper llmAuditMapper;
    
    @Override
    public Long recordAudit(Long userId, String originalQuestion, String recalledTables, String generatedSql, Long executionTime)
    {
        return recordAudit(userId, originalQuestion, recalledTables, generatedSql, executionTime, null, null, null);
    }

    @Override
    public Long recordAudit(Long userId, String originalQuestion, String recalledTables, String generatedSql, Long executionTime,
                           String promptVersion, String metaSchemaVersion, String errorCategory)
    {
        return recordAudit(userId, originalQuestion, recalledTables, generatedSql, executionTime, promptVersion, metaSchemaVersion, errorCategory, null, null);
    }

    @Override
    public Long recordAudit(Long userId, String originalQuestion, String recalledTables, String generatedSql, Long executionTime,
                           String promptVersion, String metaSchemaVersion, String errorCategory, Integer retryCount, String finalSql)
    {
        if (llmAuditMapper == null)
        {
            log.warn("LlmAuditMapper未配置，跳过审计记录");
            return null;
        }
        
        try
        {
            LlmAudit audit = new LlmAudit();
            audit.setUserId(userId);
            audit.setOriginalQuestion(originalQuestion);
            audit.setRecalledTables(StringUtils.isNotEmpty(recalledTables) ? recalledTables : null);
            audit.setGeneratedSql(generatedSql);
            audit.setResponseTime(executionTime);
            audit.setCreateTime(DateUtils.getNowDate());
            audit.setPromptVersion(promptVersion);
            audit.setMetaSchemaVersion(metaSchemaVersion);
            audit.setErrorCategory(errorCategory);
            audit.setRetryCount(retryCount);
            audit.setFinalSql(finalSql);
            
            int result = llmAuditMapper.insertLlmAudit(audit);
            if (result > 0)
            {
                log.debug("LLM请求审计记录已保存: id={}, userId={}", audit.getId(), userId);
                return audit.getId();
            }
            else
            {
                log.warn("LLM请求审计记录保存失败: userId={}", userId);
                return null;
            }
        }
        catch (Exception e)
        {
            log.error("保存LLM请求审计记录失败: userId={}", userId, e);
            return null;
        }
    }
    
    @Override
    public String desensitizePII(String question)
    {
        if (StringUtils.isEmpty(question))
        {
            return question;
        }
        
        try
        {
            String desensitized = question;
            
            // 脱敏手机号
            desensitized = PHONE_PATTERN.matcher(desensitized).replaceAll(matchResult -> {
                String phone = matchResult.group();
                return DesensitizeUtils.desensitizePhone(phone);
            });
            
            // 脱敏身份证号
            desensitized = ID_CARD_PATTERN.matcher(desensitized).replaceAll(matchResult -> {
                String idCard = matchResult.group();
                return DesensitizeUtils.desensitizeIdCard(idCard);
            });
            
            // 如果进行了脱敏，记录日志
            if (!desensitized.equals(question))
            {
                log.debug("PII脱敏完成: originalLength={}, desensitizedLength={}", question.length(), desensitized.length());
            }
            
            return desensitized;
        }
        catch (Exception e)
        {
            log.warn("PII脱敏失败，返回原始问题: question={}", question, e);
            return question;
        }
    }
    
    @Override
    public LlmAudit selectLlmAuditById(Long id)
    {
        if (llmAuditMapper == null || id == null)
        {
            return null;
        }
        
        try
        {
            return llmAuditMapper.selectLlmAuditById(id);
        }
        catch (Exception e)
        {
            log.error("查询LLM审计记录失败: id={}", id, e);
            return null;
        }
    }

    @Override
    public List<LlmAudit> listErrorsSince(java.util.Date startTime)
    {
        if (llmAuditMapper == null || startTime == null)
        {
            return java.util.Collections.emptyList();
        }
        try
        {
            return llmAuditMapper.selectLlmAuditListSince(startTime);
        }
        catch (Exception e)
        {
            log.error("查询时间窗口内审计错误记录失败: startTime={}", startTime, e);
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public List<LlmAudit> listForAmbiguity(String errorCategory, String tableName,
                                           java.util.Date startTime, java.util.Date endTime, String processStatus)
    {
        if (llmAuditMapper == null)
            return java.util.Collections.emptyList();
        try
        {
            return llmAuditMapper.selectLlmAuditListForAmbiguity(errorCategory, tableName, startTime, endTime, processStatus);
        }
        catch (Exception e)
        {
            log.error("歧义列表查询失败", e);
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public int resolveAmbiguity(Long id)
    {
        if (llmAuditMapper == null || id == null)
            return 0;
        LlmAudit audit = new LlmAudit();
        audit.setId(id);
        audit.setProcessStatus("RESOLVED");
        return llmAuditMapper.updateLlmAudit(audit);
    }

    @Override
    public int bindQueryRecord(Long auditId, Long queryRecordId)
    {
        if (llmAuditMapper == null || auditId == null || queryRecordId == null)
        {
            return 0;
        }
        LlmAudit audit = new LlmAudit();
        audit.setId(auditId);
        audit.setQueryRecordId(queryRecordId);
        return llmAuditMapper.updateLlmAudit(audit);
    }

    @Override
    public int updateRetryInfo(Long auditId, int retryCount, String finalSql)
    {
        if (llmAuditMapper == null || auditId == null)
            return 0;
        LlmAudit audit = new LlmAudit();
        audit.setId(auditId);
        audit.setRetryCount(retryCount);
        audit.setFinalSql(finalSql);
        return llmAuditMapper.updateLlmAudit(audit);
    }

    @Override
    public List<TableErrorSummary> getAmbiguitySummary(java.util.Date startTime, java.util.Date endTime)
    {
        if (llmAuditMapper == null)
            return java.util.Collections.emptyList();
        java.util.Date start = startTime;
        java.util.Date end = endTime;
        if (end == null) end = DateUtils.getNowDate();
        if (start == null)
        {
            Calendar cal = Calendar.getInstance();
            cal.setTime(end);
            cal.add(Calendar.DAY_OF_MONTH, -90);
            start = cal.getTime();
        }
        try
        {
            List<LlmAudit> list = llmAuditMapper.selectLlmAuditListForAmbiguitySummary(start, end);
            Map<String, Map<String, Integer>> tableToCategories = new TreeMap<>();
            for (LlmAudit a : list)
            {
                Set<String> tables = parseTableNames(a.getRecalledTables(), a.getGeneratedSql());
                String category = StringUtils.isEmpty(a.getErrorCategory()) ? "OTHER" : a.getErrorCategory();
                for (String table : tables)
                {
                    if (StringUtils.isEmpty(table)) continue;
                    table = table.trim();
                    tableToCategories.computeIfAbsent(table, k -> new LinkedHashMap<>()).merge(category, 1, Integer::sum);
                }
            }
            List<TableErrorSummary> result = new ArrayList<>();
            for (Map.Entry<String, Map<String, Integer>> e : tableToCategories.entrySet())
            {
                TableErrorSummary s = new TableErrorSummary();
                s.setTableName(e.getKey());
                int total = e.getValue().values().stream().mapToInt(Integer::intValue).sum();
                s.setTotalCount(total);
                List<TableErrorSummary.CategoryCount> cats = new ArrayList<>();
                e.getValue().entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .forEach(ce -> {
                        TableErrorSummary.CategoryCount cc = new TableErrorSummary.CategoryCount();
                        cc.setErrorCategory(ce.getKey());
                        cc.setCount(ce.getValue());
                        cats.add(cc);
                    });
                s.setCategories(cats);
                result.add(s);
            }
            result.sort((a, b) -> Integer.compare(b.getTotalCount(), a.getTotalCount()));
            return result;
        }
        catch (Exception e)
        {
            log.error("歧义汇总查询失败", e);
            return java.util.Collections.emptyList();
        }
    }

    private static Set<String> parseTableNames(String recalled, String sql)
    {
        Set<String> set = new LinkedHashSet<>();
        if (StringUtils.isNotEmpty(recalled))
        {
            for (String s : recalled.split(","))
            {
                String t = s.trim();
                if (!t.isEmpty()) set.add(t);
            }
        }
        if (StringUtils.isNotEmpty(sql))
        {
            Matcher m = TABLE_IN_SQL_PATTERN.matcher(sql);
            while (m.find()) set.add(m.group(1));
        }
        return set;
    }
}
