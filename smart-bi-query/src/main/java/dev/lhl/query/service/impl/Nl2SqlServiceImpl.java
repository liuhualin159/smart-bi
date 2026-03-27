package dev.lhl.query.service.impl;

import com.alibaba.fastjson2.JSON;
import dev.lhl.metadata.domain.AtomicMetric;
import dev.lhl.metadata.domain.FieldMetadata;
import dev.lhl.metadata.domain.TableMetadata;
import dev.lhl.metadata.domain.TableRelation;
import dev.lhl.datasource.service.IDataSourceService;
import dev.lhl.metadata.service.IMetadataService;
import dev.lhl.metadata.service.VectorSearchService;
import dev.lhl.query.domain.FeedbackCorrection;
import dev.lhl.query.domain.QueryRecord;
import dev.lhl.query.service.INl2SqlService;
import dev.lhl.query.service.ITableMetadataToolService;
import dev.lhl.query.service.LlmService;
import dev.lhl.query.service.PromptTemplateService;
import dev.lhl.query.service.ILlmAuditService;
import dev.lhl.common.utils.SqlSecurityUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.prompt.Prompt;
import dev.lhl.query.service.SqlSyntaxPrecheckService;
import dev.lhl.query.service.FewshotRetrievalService;

/**
 * NL2SQL服务实现
 * 使用Spring AI框架调用LLM生成SQL
 * 
 * @author smart-bi
 */
@Service
public class Nl2SqlServiceImpl implements INl2SqlService
{
    private static final Logger log = LoggerFactory.getLogger(Nl2SqlServiceImpl.class);

    @Autowired
    private LlmService llmService;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired(required = false)
    private VectorSearchService vectorSearchService;

    @Autowired(required = false)
    private IMetadataService metadataService;

    @Autowired(required = false)
    private ITableMetadataToolService tableMetadataToolService;

    @Autowired
    private PermissionInjectionService permissionInjectionService;
    
    @Autowired(required = false)
    private ILlmAuditService llmAuditService;
    
    @Autowired(required = false)
    private dev.lhl.query.service.IConversationService conversationService;

    @Autowired(required = false)
    private dev.lhl.query.service.IQuestionPreprocessService questionPreprocessService;

    @Autowired(required = false)
    private dev.lhl.query.service.IFeedbackCorrectionService feedbackCorrectionService;

    @Autowired(required = false)
    private IDataSourceService dataSourceService;

    @Autowired(required = false)
    private SqlSyntaxPrecheckService syntaxPrecheckService;

    @Autowired(required = false)
    private FewshotRetrievalService fewshotRetrievalService;

    @Value("${smart.bi.feedback.similarity.threshold:0.8}")
    private double feedbackSimilarityThreshold;

    @Value("${smart.bi.nl2sql.selfCorrection.enabled:true}")
    private boolean selfCorrectionEnabled;

    @Value("${smart.bi.nl2sql.selfCorrection.maxRetries:2}")
    private int selfCorrectionMaxRetries;

    @Value("${smart.bi.nl2sql.confidence.threshold:0.6}")
    private double confidenceThreshold;

    @Override
    public QueryRecord generateSQL(String question, Long userId, Long sessionId)
    {
        String generatedSql = null;
        String recalledTables = "";
        long llmExecutionTime = 0L;
        String metaSchemaVersion = "1.0";
        int retryCount = 0;
        String tableStructures = null;
        try
        {
            log.info("开始生成SQL: question={}, userId={}, sessionId={}", question, userId, sessionId);
            
            // 1. 输入验证
            validateQuestion(question);

            // 1.5 向量依赖可用性检查（不可用时明确失败原因）
            ensureVectorDependenciesReady(question);
            
            // 2. 问题预处理（纠错与意图归一化，失败时降级返回原文）
            if (questionPreprocessService != null)
            {
                question = questionPreprocessService.preprocess(question);
            }
            
            // 3. PII脱敏（发送给LLM前）
            String desensitizedQuestion = question;
            if (llmAuditService != null)
            {
                desensitizedQuestion = llmAuditService.desensitizePII(question);
            }
            
            // 4. 获取表结构与指标定义（向量检索 + 元数据服务）
            tableStructures = retrieveTableStructures(desensitizedQuestion);
            String metricDefinitions = retrieveMetricDefinitions(desensitizedQuestion);
            recalledTables = extractTableNamesFromStructures(tableStructures);
            
            // 5. 获取对话上下文（如果有sessionId）
            String conversationContext = "";
            if (sessionId != null && conversationService != null)
            {
                try
                {
                    List<Map<String, String>> contextList = conversationService.getContextForLLM(sessionId);
                    if (contextList != null && !contextList.isEmpty())
                    {
                        // 将上下文转换为文本格式
                        StringBuilder contextBuilder = new StringBuilder();
                        contextBuilder.append("之前的对话历史：\n");
                        for (Map<String, String> qa : contextList)
                        {
                            contextBuilder.append("问：").append(qa.get("question")).append("\n");
                            contextBuilder.append("答：").append(qa.get("answer")).append("\n\n");
                        }
                        conversationContext = contextBuilder.toString();
                        log.debug("获取对话上下文: sessionId={}, contextSize={}", sessionId, contextList.size());
                    }
                }
                catch (Exception e)
                {
                    log.warn("获取对话上下文失败: sessionId={}", sessionId, e);
                }
            }
            
            // 6. 获取全部可用表列表（仅白名单 NORMAL/PREFERRED），约束 LLM 禁止臆造表名
            String allTablesList = "";
            if (tableMetadataToolService != null)
            {
                allTablesList = tableMetadataToolService.formatAllTablesListForPrompt();
            }
            // 7. 推荐 JOIN 与敏感字段规则
            String recommendedJoins = buildRecommendedJoins();
            String sensitiveFieldRules = "exposure_policy=FORBIDDEN 的字段仅可用于 WHERE/JOIN；exposure_policy=AGG_ONLY 的字段仅可用于聚合结果。";
            boolean hasTemporalOrRatioIntent = containsTemporalOrRatioIntent(desensitizedQuestion);
            String feedbackCorrectionsHint = "";
            if (feedbackCorrectionService != null)
            {
                List<FeedbackCorrection> similar = feedbackCorrectionService.findSimilarCorrections(desensitizedQuestion, feedbackSimilarityThreshold);
                if (!similar.isEmpty())
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("\n        - **相似问题参考（审核通过的正确 SQL，优先采用）**：\n");
                    for (FeedbackCorrection fc : similar)
                    {
                        sb.append("          问题：「").append(fc.getOriginalQuestion()).append("」 → SQL: ").append(fc.getCorrectedSql()).append("\n");
                    }
                    feedbackCorrectionsHint = sb.toString();
                }
            }
            // 8. 获取目标数据库版本并注入提示词（避免生成不兼容语法，如表别名 TO 等保留字）
            String databaseVersion = null;
            if (dataSourceService != null)
            {
                try
                {
                    databaseVersion = dataSourceService.getLocalDatabaseVersion();
                }
                catch (Exception e)
                {
                    log.warn("获取数据库版本失败，将使用默认提示", e);
                }
            }
            // 8.5 Few-shot 示例检索与注入
            String fewshotBlock = "";
            if (fewshotRetrievalService != null)
            {
                try
                {
                    List<Map<String, String>> fewshotExamples = fewshotRetrievalService.retrieveExamples(desensitizedQuestion, null);
                    fewshotBlock = promptTemplateService.formatFewshotExamples(fewshotExamples);
                }
                catch (Exception e)
                {
                    log.warn("Few-shot 示例检索失败，降级为不注入", e);
                }
            }

            // 9. 构建提示词（含数据库版本、全部可用表、相关表结构、指标定义、推荐 JOIN、敏感规则和上下文）
            long promptStartTime = System.currentTimeMillis();
            metaSchemaVersion = "1.0";
            Prompt prompt = promptTemplateService.createNl2SqlPrompt(
                desensitizedQuestion,
                "用户ID: " + userId,
                allTablesList,
                tableStructures,
                metricDefinitions,
                conversationContext,
                recommendedJoins,
                sensitiveFieldRules,
                hasTemporalOrRatioIntent,
                feedbackCorrectionsHint,
                databaseVersion,
                fewshotBlock
            );
            
            // 9. 调用LLM生成SQL（输出 JSON 含 sql/confidence/disambiguationQuestions）
            String rawResponse = llmService.callPrompt(prompt);
            llmExecutionTime = System.currentTimeMillis() - promptStartTime;
            Double confidence = 1.0;
            List<String> disambiguationList = List.of();
            
            if (rawResponse != null && !rawResponse.trim().isEmpty())
            {
                String trimmed = rawResponse.trim();
                if (trimmed.startsWith("```")) { int s = trimmed.indexOf("{"); int e = trimmed.lastIndexOf("}"); if (s >= 0 && e > s) trimmed = trimmed.substring(s, e + 1); }
                try
                {
                    com.alibaba.fastjson2.JSONObject obj = JSON.parseObject(trimmed);
                    if (obj != null)
                    {
                        generatedSql = obj.getString("sql");
                        Object conf = obj.get("confidence");
                        if (conf != null && conf instanceof Number) confidence = ((Number) conf).doubleValue();
                        Object qs = obj.get("disambiguationQuestions");
                        if (qs != null && qs instanceof List)
                        {
                            @SuppressWarnings("unchecked")
                            List<String> ql = (List<String>) qs;
                            disambiguationList = ql != null ? ql : List.of();
                        }
                    }
                }
                catch (Exception e) { log.debug("NL2SQL JSON 解析失败，尝试从回复中提取 SQL: {}", e.getMessage()); }
                if (generatedSql == null || generatedSql.trim().isEmpty())
                {
                    generatedSql = extractSqlFromRawResponse(rawResponse);
                }
            }

            // 9.5 若模型给出澄清问题（或低置信度且未给出 SQL），则直接返回澄清结果，不做安全校验/权限注入
            boolean hasDisambiguation = disambiguationList != null && !disambiguationList.isEmpty();
            boolean sqlBlank = generatedSql == null || generatedSql.trim().isEmpty();
            boolean lowConfidence = confidence != null && confidence < confidenceThreshold;
            if (hasDisambiguation || (lowConfidence && sqlBlank))
            {
                QueryRecord record = new QueryRecord();
                record.setSessionId(sessionId);
                record.setUserId(userId);
                record.setQuestion(question);
                record.setGeneratedSql(sqlBlank ? "" : generatedSql);
                record.setExecutedSql(null);
                record.setConfidence(confidence);
                record.setDisambiguationQuestions(hasDisambiguation ? JSON.toJSONString(disambiguationList) : null);
                record.setInvolvedTables(null);
                record.setStatus("SUCCESS");

                // 澄清场景也写入对话上下文，避免下一轮重复提出相同澄清问题
                if (sessionId != null && conversationService != null)
                {
                    try
                    {
                        String qs = (disambiguationList != null && !disambiguationList.isEmpty())
                            ? String.join("；", disambiguationList)
                            : "需要补充信息";
                        conversationService.updateContext(sessionId, question, "需要澄清：" + qs);
                    }
                    catch (Exception ex)
                    {
                        log.warn("更新对话上下文失败(澄清场景): sessionId={}", sessionId, ex);
                    }
                }

                // 记录LLM审计（澄清场景也记录，便于分析）
                if (llmAuditService != null)
                {
                    try
                    {
                        Long id = llmAuditService.recordAudit(
                            userId,
                            desensitizedQuestion,
                            recalledTables,
                            record.getGeneratedSql() != null ? record.getGeneratedSql() : "",
                            llmExecutionTime,
                            "v1",
                            metaSchemaVersion,
                            null,
                            0,
                            null
                        );
                        record.setAuditId(id);
                    }
                    catch (Exception e)
                    {
                        log.warn("记录LLM审计失败", e);
                    }
                }

                log.info("触发澄清返回: question={}, confidence={}, questions={}", question, confidence, disambiguationList != null ? disambiguationList.size() : 0);
                return record;
            }
            
            // 10. SQL安全校验
            SqlSecurityUtils.validateSQL(generatedSql);

            // 10.5 SQL语法预检（安全校验通过后、执行前）
            if (syntaxPrecheckService != null)
            {
                SqlSyntaxPrecheckService.PrecheckResult precheckResult = syntaxPrecheckService.check(generatedSql);
                if (!precheckResult.passed())
                {
                    // 语法预检失败，尝试自修正（将预检错误作为 DB 错误）
                    if (selfCorrectionEnabled && selfCorrectionMaxRetries > 0)
                    {
                        log.info("语法预检失败，尝试自修正: {}", precheckResult.errorMessage());
                        String correctedSql = attemptSelfCorrection(desensitizedQuestion, generatedSql, precheckResult.errorMessage(), tableStructures);
                        if (correctedSql != null)
                        {
                            retryCount++;
                            generatedSql = correctedSql;
                            SqlSecurityUtils.validateSQL(generatedSql);
                        }
                        else
                        {
                            throw new RuntimeException(precheckResult.errorMessage());
                        }
                    }
                    else
                    {
                        throw new RuntimeException(precheckResult.errorMessage());
                    }
                }
            }

            // 11. 权限注入
            String executedSql = permissionInjectionService.injectPermissions(generatedSql, userId);
            
            // 12. 更新对话上下文（如果有sessionId）
            if (sessionId != null && conversationService != null)
            {
                try
                {
                    conversationService.updateContext(sessionId, question, "生成的SQL: " + generatedSql);
                }
                catch (Exception ex)
                {
                    log.warn("更新对话上下文失败: sessionId={}", sessionId, ex);
                }
            }
            
            // 13. 创建查询记录
            QueryRecord record = new QueryRecord();
            record.setSessionId(sessionId);
            record.setUserId(userId);
            record.setQuestion(question);
            record.setGeneratedSql(generatedSql);
            record.setExecutedSql(executedSql);
            record.setConfidence(confidence);
            record.setDisambiguationQuestions(disambiguationList.isEmpty() ? null : JSON.toJSONString(disambiguationList));
            record.setInvolvedTables(extractTableNamesFromSql(generatedSql));
            record.setStatus("SUCCESS");
            
            // 14. 记录LLM审计（含 prompt_version、meta_schema_version、retry_count、final_sql）
            if (llmAuditService != null)
            {
                try
                {
                    String finalSql = retryCount > 0 ? generatedSql : null;
                    Long id = llmAuditService.recordAudit(userId, desensitizedQuestion, recalledTables, generatedSql, llmExecutionTime, "v1", metaSchemaVersion, null, retryCount, finalSql);
                    record.setAuditId(id);
                }
                catch (Exception e)
                {
                    log.warn("记录LLM审计失败", e);
                }
            }
            
            log.info("SQL生成成功: question={}, sql={}", question, generatedSql);
            return record;
        }
        catch (Exception e)
        {
            log.error("SQL生成失败: question={}", question, e);
            QueryRecord record = new QueryRecord();
            record.setSessionId(sessionId);
            record.setUserId(userId);
            record.setQuestion(question);
            record.setStatus("FAILED");
            record.setErrorMessage(e.getMessage());
            if (llmAuditService != null)
            {
                try
                {
                    String finalSql = retryCount > 0 ? generatedSql : null;
                    Long id = llmAuditService.recordAudit(userId, question != null ? question : "",
                            recalledTables != null ? recalledTables : "", generatedSql != null ? generatedSql : "",
                            llmExecutionTime, "v1", metaSchemaVersion != null ? metaSchemaVersion : "1.0", "OTHER", retryCount, finalSql);
                    record.setAuditId(id);
                }
                catch (Exception ex) { log.warn("记录失败审计异常", ex); }
            }
            return record;
        }
    }

    /**
     * 自修正：SQL 执行失败后，将错误信息反馈给 LLM 重新生成
     *
     * @return 修正后的 SQL，或 null 表示修正失败
     */
    private String attemptSelfCorrection(String question, String failedSql, String errorMessage, String tableStructures)
    {
        for (int i = 0; i < selfCorrectionMaxRetries; i++)
        {
            try
            {
                log.info("自修正第 {} 次: question={}, error={}", i + 1, question, errorMessage);
                Prompt correctionPrompt = promptTemplateService.createSqlCorrectionPrompt(question, failedSql, errorMessage, tableStructures);
                String rawResponse = llmService.callPrompt(correctionPrompt);
                if (rawResponse == null || rawResponse.trim().isEmpty())
                {
                    log.warn("自修正第 {} 次：LLM 返回空", i + 1);
                    continue;
                }
                String correctedSql = extractSqlFromRawResponse(rawResponse);
                if (correctedSql == null || correctedSql.trim().isEmpty())
                {
                    log.warn("自修正第 {} 次：无法提取 SQL", i + 1);
                    continue;
                }

                // 安全校验
                try
                {
                    SqlSecurityUtils.validateSQL(correctedSql);
                }
                catch (Exception e)
                {
                    log.warn("自修正第 {} 次：安全校验失败: {}", i + 1, e.getMessage());
                    continue;
                }

                // 语法预检
                if (syntaxPrecheckService != null)
                {
                    SqlSyntaxPrecheckService.PrecheckResult precheck = syntaxPrecheckService.check(correctedSql);
                    if (!precheck.passed())
                    {
                        log.warn("自修正第 {} 次：语法预检仍失败: {}", i + 1, precheck.errorMessage());
                        failedSql = correctedSql;
                        errorMessage = precheck.errorMessage();
                        continue;
                    }
                }

                log.info("自修正第 {} 次成功: correctedSql={}", i + 1, correctedSql);
                return correctedSql;
            }
            catch (Exception e)
            {
                log.warn("自修正第 {} 次异常", i + 1, e);
            }
        }
        log.warn("自修正 {} 次均失败", selfCorrectionMaxRetries);
        return null;
    }

    /**
     * 供 Controller 调用：当查询执行失败时尝试自修正 SQL
     *
     * @param question 用户原始问题
     * @param failedSql 失败的 SQL
     * @param dbErrorMessage 数据库错误信息
     * @param userId 用户ID
     * @return 修正后的 SQL，或 null
     */
    public String correctSQL(String question, String failedSql, String dbErrorMessage, Long userId)
    {
        if (!selfCorrectionEnabled || selfCorrectionMaxRetries <= 0)
        {
            return null;
        }
        String tableStructures = retrieveTableStructures(question);
        return attemptSelfCorrection(question, failedSql, dbErrorMessage, tableStructures);
    }

    @Override
    public QueryRecord correctAndPrepareForRetry(String question, QueryRecord currentRecord, String dbErrorMessage, Long userId)
    {
        if (!selfCorrectionEnabled || selfCorrectionMaxRetries <= 0)
        {
            return null;
        }
        String failedSql = currentRecord != null ? currentRecord.getGeneratedSql() : null;
        if (failedSql == null || failedSql.trim().isEmpty())
        {
            return null;
        }
        String tableStructures = retrieveTableStructures(question);
        String correctedSql = attemptSelfCorrection(question, failedSql, dbErrorMessage, tableStructures);
        if (correctedSql == null || correctedSql.trim().isEmpty())
        {
            return null;
        }
        try
        {
            SqlSecurityUtils.validateSQL(correctedSql);
        }
        catch (Exception e)
        {
            log.warn("自修正后安全校验不通过，不重试: {}", e.getMessage());
            return null;
        }
        if (syntaxPrecheckService != null)
        {
            SqlSyntaxPrecheckService.PrecheckResult precheck = syntaxPrecheckService.check(correctedSql);
            if (!precheck.passed())
            {
                log.warn("自修正后语法预检仍失败，不重试: {}", precheck.errorMessage());
                return null;
            }
        }
        try
        {
            String executedSql = permissionInjectionService.injectPermissions(correctedSql, userId);
            currentRecord.setGeneratedSql(correctedSql);
            currentRecord.setExecutedSql(executedSql);
            currentRecord.setInvolvedTables(extractTableNamesFromSql(correctedSql));
            return currentRecord;
        }
        catch (Exception e)
        {
            log.warn("自修正后权限注入失败，不重试: {}", e.getMessage());
            return null;
        }
    }

    private static Long toLong(Object v)
    {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    private void validateQuestion(String question)
    {
        if (question == null || question.trim().isEmpty())
        {
            throw new RuntimeException("问题不能为空");
        }
        if (question.length() > 500)
        {
            throw new RuntimeException("问题长度不能超过500字符");
        }
    }

    /**
     * 在智能问数执行前检查向量库与向量模型可用性。
     * 要求：服务可启动，但当向量依赖不可用时阻止智能问数并给出友好提示。
     */
    private void ensureVectorDependenciesReady(String question)
    {
        if (vectorSearchService == null)
        {
            throw new RuntimeException("向量库不可用，无法执行智能问数");
        }
        try
        {
            // 使用真实检索链路做探测：同时覆盖向量库连通性与向量模型调用可用性。
            vectorSearchService.search(question, 1);
        }
        catch (Exception e)
        {
            String message = e.getMessage() != null ? e.getMessage() : "";
            if (isEmbeddingUnavailable(message))
            {
                throw new RuntimeException("向量模型不可用，无法执行智能问数");
            }
            throw new RuntimeException("向量库不可用，无法执行智能问数");
        }
    }

    private boolean isEmbeddingUnavailable(String message)
    {
        String m = message == null ? "" : message.toLowerCase();
        return m.contains("embedding")
            || m.contains("embeddings")
            || m.contains("api-key")
            || m.contains("unauthorized")
            || m.contains("forbidden")
            || m.contains("http 401")
            || m.contains("http 403")
            || m.contains("http 404");
    }

    /**
     * 检测问题是否包含时间对比或占比意图（同比、环比、占比等）
     */
    private boolean containsTemporalOrRatioIntent(String question)
    {
        if (question == null || question.isEmpty()) return false;
        String q = question.trim();
        return q.contains("同比") || q.contains("环比") || q.contains("占比")
            || q.contains("和上期比较") || q.contains("和去年对比") || q.contains("和上月对比")
            || q.contains("增长率") || q.contains("与上月") || q.contains("与去年");
    }

    /**
     * 从 LLM 原始回复中提取 SQL（当 JSON 解析失败时的兜底）
     */
    private static String extractSqlFromRawResponse(String raw)
    {
        if (raw == null || raw.trim().isEmpty()) return null;
        String t = raw.trim();
        Pattern block = Pattern.compile("```(?:sql)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher m = block.matcher(t);
        if (m.find()) { String s = m.group(1).trim(); if (!s.isEmpty()) return s; }
        Pattern select = Pattern.compile("(SELECT\\s+[\\s\\S]*?)(?:;\\s*$|;\\s*\\n|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        m = select.matcher(t);
        if (m.find()) { String s = m.group(1).trim(); if (!s.isEmpty()) return s; }
        return t;
    }

    /**
     * 从 SQL 中提取涉及的表名（FROM、JOIN）
     */
    private static String extractTableNamesFromSql(String sql)
    {
        if (sql == null || sql.trim().isEmpty()) return null;
        Set<String> names = new LinkedHashSet<>();
        Pattern fromJoin = Pattern.compile("(?:FROM|JOIN)\\s+([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = fromJoin.matcher(sql);
        while (m.find()) names.add(m.group(1));
        return names.isEmpty() ? null : JSON.toJSONString(new ArrayList<>(names));
    }

    /**
     * 获取表结构信息：优先向量检索，再与白名单取交集；仅使用 NORMAL/PREFERRED 表
     */
    private String retrieveTableStructures(String question)
    {
        Set<Long> tableIds = new HashSet<>();
        try
        {
            if (vectorSearchService != null)
            {
                var results = vectorSearchService.search(question, 15);
                if (results != null && !results.isEmpty())
                {
                    for (var result : results)
                    {
                        Object metaObj = result.get("metadata");
                        if (!(metaObj instanceof Map)) continue;
                        @SuppressWarnings("unchecked")
                        Map<String, Object> meta = (Map<String, Object>) metaObj;
                        String type = String.valueOf(meta.get("type"));
                        Long tid = toLong(meta.get("tableId"));
                        if (("table".equals(type) || "field".equals(type)) && tid != null) tableIds.add(tid);
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.warn("向量检索失败: question={}", question, e);
        }
        return buildTableStructuresFromMetadata(tableIds);
    }

    /**
     * 从元数据构建表结构字符串：仅白名单表（NORMAL/PREFERRED），字段裁剪（DIMENSION/MEASURE 优先，按 nl2sql_priority），并标注 exposure 规则
     */
    private String buildTableStructuresFromMetadata(Set<Long> tableIds)
    {
        if (metadataService == null) return "";
        try
        {
            List<Long> restrictIds = (tableIds == null || tableIds.isEmpty()) ? null : new ArrayList<>(tableIds);
            List<TableMetadata> tables = metadataService.selectTableMetadataListForNl2Sql(restrictIds);
            if (tables == null || tables.isEmpty())
            {
                log.debug("元数据中无 NL2SQL 白名单表");
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (TableMetadata t : tables)
            {
                sb.append("### ").append(t.getTableName());
                if (dev.lhl.common.utils.StringUtils.isNotEmpty(t.getTableComment()))
                    sb.append("（").append(t.getTableComment()).append("）");
                if ("PREFERRED".equals(t.getNl2sqlVisibilityLevel()))
                    sb.append(" [优先推荐]");
                sb.append("\n");
                if (dev.lhl.common.utils.StringUtils.isNotEmpty(t.getBusinessDescription()))
                    sb.append("业务描述: ").append(t.getBusinessDescription()).append("\n");
                List<FieldMetadata> fields = metadataService.selectFieldMetadataListByTableId(t.getId());
                List<FieldMetadata> trimmed = trimFieldsForNl2Sql(fields);
                if (!trimmed.isEmpty())
                {
                    sb.append("字段: ");
                    for (int i = 0; i < trimmed.size(); i++)
                    {
                        FieldMetadata f = trimmed.get(i);
                        sb.append(f.getFieldName());
                        if (dev.lhl.common.utils.StringUtils.isNotEmpty(f.getFieldType()))
                            sb.append("(").append(f.getFieldType()).append(")");
                        if (dev.lhl.common.utils.StringUtils.isNotEmpty(f.getSemanticType()))
                            sb.append(" 语义类型:").append(f.getSemanticType());
                        if (dev.lhl.common.utils.StringUtils.isNotEmpty(f.getBusinessAlias()))
                            sb.append(" 业务别名:").append(f.getBusinessAlias());
                        if (dev.lhl.common.utils.StringUtils.isNotEmpty(f.getBusinessDescription()))
                            sb.append(" 业务描述:").append(f.getBusinessDescription());
                        String enumMapping = formatEnumValuesForPrompt(f.getEnumValues());
                        if (dev.lhl.common.utils.StringUtils.isNotEmpty(enumMapping))
                            sb.append(" 枚举值(显示→存储):").append(enumMapping);
                        if ("FORBIDDEN".equals(f.getExposurePolicy()))
                            sb.append(" [仅WHERE/JOIN]");
                        else if ("AGG_ONLY".equals(f.getExposurePolicy()))
                            sb.append(" [仅聚合]");
                        if (i < trimmed.size() - 1) sb.append(" | ");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            log.warn("从元数据构建表结构失败", e);
            return "";
        }
    }

    /**
     * 将 enum_values(JSON) 格式化为「显示→存储」映射字符串，供 NL2SQL 提示词使用。
     * 支持格式：[{"label":"已支付","value":"1"}], {"1":"已支付"}, ["1","2"]
     */
    private String formatEnumValuesForPrompt(String enumValuesJson)
    {
        if (dev.lhl.common.utils.StringUtils.isEmpty(enumValuesJson)) return null;
        try
        {
            Object parsed = JSON.parse(enumValuesJson);
            List<String> pairs = new ArrayList<>();
            if (parsed instanceof List)
            {
                List<?> list = (List<?>) parsed;
                for (Object item : list)
                {
                    if (item instanceof Map)
                    {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> m = (Map<String, Object>) item;
                        Object label = m.get("label");
                        Object value = m.get("value");
                        if (label != null && value != null)
                            pairs.add(label + "→" + value);
                        else if (value != null)
                            pairs.add(String.valueOf(value));
                    }
                    else
                        pairs.add(String.valueOf(item));
                }
            }
            else if (parsed instanceof Map)
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) parsed;
                for (Map.Entry<String, Object> e : m.entrySet())
                    pairs.add(e.getValue() + "→" + e.getKey());
            }
            return pairs.isEmpty() ? null : String.join(", ", pairs);
        }
        catch (Exception e)
        {
            log.debug("解析枚举值失败: {}", enumValuesJson, e);
            return null;
        }
    }

    /** 字段裁剪：优先 DIMENSION/MEASURE，按 nl2sql_priority 降序，每表最多 40 个 */
    private List<FieldMetadata> trimFieldsForNl2Sql(List<FieldMetadata> fields)
    {
        if (fields == null || fields.isEmpty()) return List.of();
        List<FieldMetadata> list = fields.stream()
            .filter(f -> f.getUsageType() == null || "DIMENSION".equals(f.getUsageType()) || "MEASURE".equals(f.getUsageType()) || "OTHER".equals(f.getUsageType()))
            .sorted(Comparator.comparing(FieldMetadata::getNl2sqlPriority, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(40)
            .collect(Collectors.toList());
        return list;
    }

    /** 构建推荐 JOIN 文本（供提示词） */
    private String buildRecommendedJoins()
    {
        if (metadataService == null) return "";
        try
        {
            List<TableRelation> relations = metadataService.selectTableRelationList();
            if (relations == null || relations.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (TableRelation r : relations)
            {
                if (dev.lhl.common.utils.StringUtils.isEmpty(r.getLeftTable()) || dev.lhl.common.utils.StringUtils.isEmpty(r.getRightTable())) continue;
                sb.append("- ").append(r.getLeftTable()).append(".").append(r.getLeftField() != null ? r.getLeftField() : "")
                    .append(" = ").append(r.getRightTable()).append(".").append(r.getRightField() != null ? r.getRightField() : "").append("\n");
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            log.warn("构建推荐 JOIN 失败", e);
            return "";
        }
    }

    /**
     * 获取指标定义：优先向量检索，无结果时从元数据服务获取全部
     */
    private String retrieveMetricDefinitions(String question)
    {
        Set<Long> metricIds = new HashSet<>();
        try
        {
            if (vectorSearchService != null)
            {
                var results = vectorSearchService.search(question, 15);
                if (results != null && !results.isEmpty())
                {
                    for (var result : results)
                    {
                        Object metaObj = result.get("metadata");
                        if (!(metaObj instanceof Map)) continue;
                        @SuppressWarnings("unchecked")
                        Map<String, Object> meta = (Map<String, Object>) metaObj;
                        if (!"atomic_metric".equals(String.valueOf(meta.get("type")))) continue;
                        Long mid = toLong(meta.get("metricId"));
                        if (mid != null) metricIds.add(mid);
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.warn("向量检索指标失败: question={}", question, e);
        }
        return buildMetricDefinitionsFromMetadata(metricIds);
    }

    private String buildMetricDefinitionsFromMetadata(Set<Long> metricIds)
    {
        if (metadataService == null) return "";
        try
        {
            List<AtomicMetric> metrics;
            if (metricIds.isEmpty())
            {
                metrics = metadataService.selectAtomicMetricList(new AtomicMetric());
            }
            else
            {
                metrics = new java.util.ArrayList<>();
                for (Long id : metricIds)
                {
                    AtomicMetric m = metadataService.selectAtomicMetricById(id);
                    if (m != null) metrics.add(m);
                }
            }
            if (metrics == null || metrics.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (AtomicMetric m : metrics)
            {
                sb.append("- ").append(m.getName());
                if (dev.lhl.common.utils.StringUtils.isNotEmpty(m.getCode()))
                    sb.append(" [").append(m.getCode()).append("]");
                sb.append(": ");
                if (dev.lhl.common.utils.StringUtils.isNotEmpty(m.getExpression()))
                    sb.append("expression=").append(m.getExpression());
                if (dev.lhl.common.utils.StringUtils.isNotEmpty(m.getDescription()))
                    sb.append(", 描述:").append(m.getDescription());
                sb.append("\n");
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            log.warn("从元数据构建指标定义失败", e);
            return "";
        }
    }
    
    /**
     * 从表结构字符串中提取表名列表（用于审计等）
     */
    private String extractTableNamesFromStructures(String tableStructures)
    {
        if (dev.lhl.common.utils.StringUtils.isEmpty(tableStructures))
        {
            return "";
        }
        try
        {
            Set<String> tableNames = new HashSet<>();
            String[] lines = tableStructures.split("\n");
            for (String line : lines)
            {
                // 支持 ### tableName 或 表名: tableName 格式
                if (line.startsWith("### "))
                {
                    String rest = line.substring(4).trim();
                    int p = rest.indexOf("（");
                    String name = p > 0 ? rest.substring(0, p).trim() : rest;
                    if (!name.isEmpty()) tableNames.add(name);
                }
                else if (line.contains("表名:"))
                {
                    int start = line.indexOf("表名:") + 3;
                    int end = line.indexOf(",", start);
                    if (end < 0) end = line.length();
                    String name = line.substring(start, end).trim();
                    if (!name.isEmpty()) tableNames.add(name);
                }
            }
            return String.join(",", tableNames);
        }
        catch (Exception e)
        {
            log.warn("提取表名失败", e);
            return "";
        }
    }
}
