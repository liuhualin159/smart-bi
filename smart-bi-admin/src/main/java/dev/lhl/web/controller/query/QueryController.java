package dev.lhl.web.controller.query;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import dev.lhl.common.core.controller.BaseController;
import dev.lhl.common.core.domain.AjaxResult;
import dev.lhl.common.core.page.TableDataInfo;
import dev.lhl.query.domain.QueryRecord;
import dev.lhl.query.service.INl2SqlService;
import dev.lhl.query.service.IQueryExecutionService;
import dev.lhl.query.service.IFilterRecommendService;
import dev.lhl.query.service.IChartRecommendService;
import dev.lhl.query.service.IChartConfigService;
import dev.lhl.query.service.IConversationService;
import dev.lhl.query.service.IQuerySuggestService;
import dev.lhl.query.service.IFeedbackService;
import dev.lhl.query.service.IAsyncQueryService;
import dev.lhl.query.service.IQuerySummarizeService;
import dev.lhl.query.service.IQueryDrillService;
import dev.lhl.query.service.ILlmAuditService;
import dev.lhl.query.domain.QuerySession;
import dev.lhl.query.domain.Feedback;
import dev.lhl.query.domain.AsyncQueryTask;
import dev.lhl.common.utils.SecurityUtils;
import dev.lhl.common.utils.StringUtils;
import dev.lhl.metadata.service.IMetadataService;
import dev.lhl.quality.service.IQualityScoreService;
import dev.lhl.quality.domain.BiQualityScore;
import dev.lhl.common.annotation.Log;
import dev.lhl.common.enums.BusinessType;
import java.util.Map;
import java.util.Collections;

/**
 * 智能查询Controller
 * 
 * @author smart-bi
 */
@RestController
@RequestMapping("/api/query")
public class QueryController extends BaseController
{
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QueryController.class);
    
    @Autowired
    private INl2SqlService nl2SqlService;
    
    @Autowired
    private IQueryExecutionService queryExecutionService;
    
    @Autowired
    private IFilterRecommendService filterRecommendService;
    
    @Autowired
    private IChartRecommendService chartRecommendService;
    
    @Autowired
    private IChartConfigService chartConfigService;
    
    @Autowired(required = false)
    private IConversationService conversationService;
    
    @Autowired(required = false)
    private IQuerySuggestService querySuggestService;
    
    @Autowired(required = false)
    private IFeedbackService feedbackService;
    
    @Autowired(required = false)
    private IAsyncQueryService asyncQueryService;

    @Autowired(required = false)
    private IQuerySummarizeService querySummarizeService;

    @Autowired(required = false)
    private IQueryDrillService queryDrillService;

    @Autowired(required = false)
    private IMetadataService metadataService;

    @Autowired(required = false)
    private IQualityScoreService qualityScoreService;

    @Value("${smart.bi.nl2sql.confidence.threshold:0.6}")
    private double confidenceThreshold;

    @Value("${smart.bi.nl2sql.selfCorrection.maxRetries:2}")
    private int selfCorrectionMaxRetries;

    @Autowired(required = false)
    private ILlmAuditService llmAuditService;

    /**
     * 自然语言查询接口
     */
    @PreAuthorize("@ss.hasPermi('query:execute')")
    @PostMapping("/execute")
    public AjaxResult executeQuery(@RequestBody Map<String, Object> params)
    {
        try
        {
            String question = (String) params.get("question");
            String sessionKey = (String) params.get("sessionKey");
            Long sessionId = params.get("sessionId") != null ? 
                Long.valueOf(params.get("sessionId").toString()) : null;
            
            Long userId = SecurityUtils.getUserId();
            
            // 1. 创建或获取会话（支持多轮对话）
            if (conversationService != null)
            {
                QuerySession session = conversationService.createOrGetSession(userId, sessionKey);
                if (session != null)
                {
                    sessionId = session.getId();
                }
            }
            
            // 2. 生成SQL（包含上下文）
            QueryRecord record = nl2SqlService.generateSQL(question, userId, sessionId);

            // 2.1 先持久化查询记录，并将审计记录绑定到 queryId（用于歧义优化页的「关联到问题」）
            persistAndBindAudit(record);

            if (!"SUCCESS".equals(record.getStatus()))
            {
                return error(record.getErrorMessage());
            }
            
            // 2.5 需要澄清时返回 needDisambiguation，不执行 SQL
            // 说明：置信度仅作为参考信号；是否进入澄清以“LLM 是否明确给出澄清问题”或“SQL 是否为空”为准，
            // 否则会出现 confidence 恰好等于阈值时不断澄清、无法执行的循环。
            boolean hasDisambiguationQuestions = StringUtils.isNotEmpty(record.getDisambiguationQuestions());
            boolean sqlBlank = StringUtils.isEmpty(record.getGeneratedSql());
            if (hasDisambiguationQuestions || sqlBlank)
            {
                // 澄清场景也需要确保已保存 queryRecord，并绑定审计记录 query_id
                persistAndBindAudit(record);
                Map<String, Object> disambigResp = new java.util.HashMap<>();
                disambigResp.put("needDisambiguation", true);
                disambigResp.put("queryRecord", record);
                List<String> questions = new java.util.ArrayList<>();
                if (hasDisambiguationQuestions)
                {
                    try
                    {
                        questions = com.alibaba.fastjson2.JSON.parseArray(record.getDisambiguationQuestions(), String.class);
                    }
                    catch (Exception e) { log.warn("解析 disambiguationQuestions 失败", e); }
                }
                disambigResp.put("disambiguationQuestions", questions);
                disambigResp.put("suggestedSql", record.getGeneratedSql());
                if (sessionId != null)
                {
                    disambigResp.put("sessionId", sessionId);
                    if (conversationService != null)
                    {
                        QuerySession session = conversationService.getSession(sessionId);
                        if (session != null) disambigResp.put("sessionKey", session.getSessionKey());
                    }
                }
                return success(disambigResp);
            }
            
            // 3. 执行查询（如果超过5秒，切换为异步任务模式）
            long queryStartTime = System.currentTimeMillis();
            IQueryExecutionService.QueryResult queryResult = null;
            Long asyncTaskId = null;
            
            try
            {
                queryResult = queryExecutionService.executeQuery(record, userId);
                
                // 执行失败且为数据库执行错误时，尝试自修正并重试（不重试安全/权限类失败）
                if (!queryResult.isSuccess() && queryResult.isRetriable() && selfCorrectionMaxRetries > 0)
                {
                    int executionRetries = 0;
                    while (executionRetries < selfCorrectionMaxRetries)
                    {
                        QueryRecord updated = nl2SqlService.correctAndPrepareForRetry(question, record, queryResult.getErrorMessage(), userId);
                        if (updated == null)
                            break;
                        record = updated;
                        executionRetries++;
                        queryResult = queryExecutionService.executeQuery(record, userId);
                        if (queryResult.isSuccess())
                            break;
                        if (!queryResult.isRetriable())
                            break;
                    }
                    if (executionRetries > 0 && record.getAuditId() != null && llmAuditService != null)
                    {
                        try
                        {
                            llmAuditService.updateRetryInfo(record.getAuditId(), executionRetries, record.getGeneratedSql());
                        }
                        catch (Exception ex)
                        {
                            log.warn("更新审计重试信息失败", ex);
                        }
                    }
                }
                
                // 检查执行时间，如果超过5秒，创建异步任务
                long executionTime = System.currentTimeMillis() - queryStartTime;
                if (executionTime > 5000 && asyncQueryService != null)
                {
                    log.info("查询执行时间超过5秒，切换为异步任务模式: executionTime={}ms", executionTime);
                    
                    // 创建异步任务
                    asyncTaskId = asyncQueryService.createAsyncTask(record, userId);
                    
                    // 返回任务信息
                    Map<String, Object> response = new java.util.HashMap<>();
                    response.put("asyncTaskId", asyncTaskId);
                    response.put("queryRecord", record);
                    response.put("isAsync", true);
                    response.put("message", "查询执行时间较长，已切换为异步任务模式");
                    
                    // 返回会话信息
                    if (sessionId != null)
                    {
                        response.put("sessionId", sessionId);
                        if (conversationService != null)
                        {
                            QuerySession session = conversationService.getSession(sessionId);
                            if (session != null)
                            {
                                response.put("sessionKey", session.getSessionKey());
                            }
                        }
                    }
                    
                    return success(response);
                }
            }
            catch (Exception e)
            {
                // 如果查询执行失败，检查是否应该创建异步任务
                long executionTime = System.currentTimeMillis() - queryStartTime;
                if (executionTime > 5000 && asyncQueryService != null && e.getMessage() != null && 
                    e.getMessage().contains("超时"))
                {
                    log.info("查询超时，切换为异步任务模式: executionTime={}ms", executionTime);
                    
                    // 创建异步任务
                    asyncTaskId = asyncQueryService.createAsyncTask(record, userId);
                    
                    Map<String, Object> response = new java.util.HashMap<>();
                    response.put("asyncTaskId", asyncTaskId);
                    response.put("queryRecord", record);
                    response.put("isAsync", true);
                    response.put("message", "查询执行超时，已切换为异步任务模式");
                    
                    if (sessionId != null)
                    {
                        response.put("sessionId", sessionId);
                    }
                    
                    return success(response);
                }
                else
                {
                    throw e;
                }
            }
            
            // 4. 更新对话上下文（记录查询结果摘要）
            if (sessionId != null && conversationService != null && queryResult.isSuccess())
            {
                try
                {
                    String answerSummary = String.format("查询成功，返回%d行数据", queryResult.getRowCount());
                    conversationService.updateContext(sessionId, question, answerSummary);
                }
                catch (Exception e)
                {
                    log.warn("更新对话上下文失败", e);
                }
            }
            
            // 5. 更新查询记录
            if (queryResult.isSuccess())
            {
                record.setStatus("SUCCESS");
                record.setDuration(queryResult.getExecutionTime());
                // 将结果转换为JSON字符串存储
                if (queryResult.getData() != null && !queryResult.getData().isEmpty())
                {
                    record.setResult(com.alibaba.fastjson2.JSON.toJSONString(queryResult.getData()));
                }
                else
                {
                    record.setResult("[]");
                }
            }
            else
            {
                record.setStatus("FAILED");
                record.setErrorMessage(queryResult.getErrorMessage());
                record.setDuration(queryResult.getExecutionTime());
            }
            
            // 6. 保存查询记录
            if (queryRecordService != null)
            {
                try
                {
                    if (record.getId() != null)
                    {
                        queryRecordService.updateQueryRecord(record);
                    }
                    else
                    {
                        queryRecordService.insertQueryRecord(record);
                    }
                    // 保存后再补一次绑定，确保拿到了 queryId
                    if (llmAuditService != null && record.getAuditId() != null && record.getId() != null)
                    {
                        try { llmAuditService.bindQueryRecord(record.getAuditId(), record.getId()); } catch (Exception ex) { log.debug("绑定审计记录 queryId 失败", ex); }
                    }
                }
                catch (Exception e)
                {
                    log.warn("保存查询记录失败", e);
                }
            }
            
            // 7. 返回结果（含质量评分）
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("queryRecord", record);
            response.put("data", queryResult.getData());
            response.put("rowCount", queryResult.getRowCount());
            response.put("executionTime", queryResult.getExecutionTime());
            response.put("qualityScores", buildQualityScores(record));
            
            // 返回会话信息（用于前端保持上下文）
            if (sessionId != null)
            {
                response.put("sessionId", sessionId);
                if (conversationService != null)
                {
                    QuerySession session = conversationService.getSession(sessionId);
                    if (session != null)
                    {
                        response.put("sessionKey", session.getSessionKey());
                    }
                }
            }
            
            return success(response);
        }
        catch (Exception e)
        {
            log.error("查询执行失败", e);
            return error("查询执行失败: " + e.getMessage());
        }
    }

    private void persistAndBindAudit(QueryRecord record)
    {
        if (record == null) return;
        if (queryRecordService == null) return;
        try
        {
            if (record.getId() != null)
            {
                queryRecordService.updateQueryRecord(record);
            }
            else
            {
                queryRecordService.insertQueryRecord(record);
            }
        }
        catch (Exception e)
        {
            log.warn("保存查询记录失败(绑定审计前置步骤)", e);
            return;
        }
        if (llmAuditService != null && record.getAuditId() != null && record.getId() != null)
        {
            try
            {
                llmAuditService.bindQueryRecord(record.getAuditId(), record.getId());
            }
            catch (Exception e)
            {
                log.debug("绑定审计记录 queryId 失败", e);
            }
        }
    }

    @Autowired
    private dev.lhl.query.service.IQueryRecordService queryRecordService;

    /** 根据 involvedTables 构建表级质量评分（用于 T052 查询结果展示） */
    private java.util.List<java.util.Map<String, Object>> buildQualityScores(QueryRecord record) {
        if (metadataService == null || qualityScoreService == null || record == null) return Collections.emptyList();
        String involved = record.getInvolvedTables();
        if (StringUtils.isEmpty(involved)) return Collections.emptyList();
        java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
        try {
            List<String> tableNames = com.alibaba.fastjson2.JSON.parseArray(involved, String.class);
            if (tableNames == null) return Collections.emptyList();
            for (String name : tableNames) {
                if (StringUtils.isEmpty(name)) continue;
                var table = metadataService.selectTableMetadataByTableName(name.trim());
                if (table == null) continue;
                BiQualityScore score = qualityScoreService.getLatestTableScore(table.getId());
                if (score != null) {
                    list.add(java.util.Map.of("tableName", name, "tableId", table.getId(), "score", score.getScore()));
                }
            }
        } catch (Exception e) {
            log.debug("解析质量评分失败: involvedTables={}", involved, e);
        }
        return list;
    }

    /**
     * 获取查询记录详情（含 generatedSql、executedSql、involvedTables，用于溯源展示）
     */
    @PreAuthorize("@ss.hasPermi('query:history')")
    @GetMapping("/record/{id}")
    public AjaxResult getQueryRecord(@PathVariable("id") Long id)
    {
        QueryRecord record = queryRecordService.selectQueryRecordById(id);
        if (record == null)
        {
            return error("查询记录不存在");
        }
        Long userId = SecurityUtils.getUserId();
        if (!record.getUserId().equals(userId) && !SecurityUtils.isAdmin(userId))
        {
            return error("无权访问该记录");
        }
        return success(record);
    }

    /**
     * 生成查询结果总结（1~3 句），与执行查询共用权限，便于智能问数数据解读
     */
    @PreAuthorize("@ss.hasPermi('query:execute')")
    @PostMapping("/summarize")
    public AjaxResult summarize(@RequestBody Map<String, Object> params)
    {
        try
        {
            Long queryId = params.get("queryId") != null ? Long.valueOf(params.get("queryId").toString()) : null;
            String chartType = (String) params.get("chartType");
            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) params.get("columns");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) params.get("data");
            if (columns == null || data == null || querySummarizeService == null)
            {
                return success(java.util.Map.of("summary", ""));
            }
            String summary = querySummarizeService.summarize(queryId, chartType, columns, data);
            return success(java.util.Map.of("summary", summary != null ? summary : ""));
        }
        catch (Exception e)
        {
            log.warn("生成总结失败", e);
            return success(java.util.Map.of("summary", ""));
        }
    }

    /**
     * 下钻：在原始 SQL 上追加 AND {dimension}={value} 后执行
     */
    @PreAuthorize("@ss.hasPermi('query:execute')")
    @PostMapping("/drill")
    public AjaxResult drill(@RequestBody Map<String, Object> params)
    {
        try
        {
            Long queryId = params.get("queryId") != null ? Long.valueOf(params.get("queryId").toString()) : null;
            String drillDimension = (String) params.get("drillDimension");
            Object drillValue = params.get("drillValue");
            if (queryId == null || StringUtils.isEmpty(drillDimension))
            {
                return error("queryId 和 drillDimension 不能为空");
            }
            if (queryDrillService == null)
            {
                return error("下钻服务未配置");
            }
            Long userId = SecurityUtils.getUserId();
            IQueryDrillService.DrillResult result = queryDrillService.drill(queryId, drillDimension, drillValue, userId);
            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("queryRecord", result.record());
            resp.put("data", result.data());
            resp.put("rowCount", result.rowCount());
            resp.put("executionTime", result.executionTime());
            return success(resp);
        }
        catch (Exception e)
        {
            log.error("下钻执行失败", e);
            return error("下钻执行失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    /**
     * 获取查询历史
     */
    @PreAuthorize("@ss.hasPermi('query:history')")
    @GetMapping("/history")
    public TableDataInfo getQueryHistory(QueryRecord queryRecord)
    {
        startPage();
        
        // 只查询当前用户的查询历史
        Long userId = SecurityUtils.getUserId();
        queryRecord.setUserId(userId);
        
        List<QueryRecord> list = queryRecordService.selectQueryRecordList(queryRecord);
        return getDataTable(list);
    }
    
    /**
     * 获取筛选器建议接口
     */
    @PreAuthorize("@ss.hasPermi('query:filter')")
    @PostMapping("/filter/recommend")
    public AjaxResult recommendFilters(@RequestBody Map<String, Object> params)
    {
        try
        {
            String question = (String) params.get("question");
            @SuppressWarnings("unchecked")
            List<String> tableNames = (List<String>) params.get("tableNames");
            
            if (question == null || question.trim().isEmpty())
            {
                return error("问题不能为空");
            }
            
            Long userId = SecurityUtils.getUserId();
            
            // 如果未提供表名，返回空列表（后续可以从SQL中提取表名）
            if (tableNames == null || tableNames.isEmpty())
            {
                tableNames = java.util.Collections.emptyList();
            }
            
            List<IFilterRecommendService.FilterConfig> filters = 
                filterRecommendService.recommendFilters(question, tableNames, userId);
            
            return success(filters);
        }
        catch (Exception e)
        {
            log.error("获取筛选器建议失败", e);
            return error("获取筛选器建议失败: " + e.getMessage());
        }
    }
    
    /**
     * 推荐图表类型接口
     */
    @PreAuthorize("@ss.hasPermi('query:chart')")
    @PostMapping("/chart/recommend")
    public AjaxResult recommendChartType(@RequestBody Map<String, Object> params)
    {
        try
        {
            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) params.get("columns");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) params.get("data");
            String question = (String) params.get("question");
            String sql = (String) params.get("sql");

            if (columns == null || columns.isEmpty() || data == null || data.isEmpty())
            {
                return error("列名或数据不能为空");
            }

            IChartRecommendService.ChartRecommendation rec =
                chartRecommendService.recommendChartTypeWithConfidence(columns, data, question, sql);
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("chartType", rec.primary().getCode());
            result.put("chartTypeName", rec.primary().getName());
            result.put("confidence", rec.confidence());
            List<String> altCodes = rec.alternatives().stream().map(IChartRecommendService.ChartType::getCode).toList();
            result.put("alternatives", altCodes);
            
            return success(result);
        }
        catch (Exception e)
        {
            log.error("推荐图表类型失败", e);
            return error("推荐图表类型失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成图表配置接口
     */
    @PreAuthorize("@ss.hasPermi('query:chart')")
    @PostMapping("/chart/config")
    public AjaxResult generateChartConfig(@RequestBody Map<String, Object> params)
    {
        try
        {
            String chartTypeStr = (String) params.get("chartType");
            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) params.get("columns");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) params.get("data");
            @SuppressWarnings("unchecked")
            Map<String, String> columnDisplayFormats = (Map<String, String>) params.get("columnDisplayFormats");

            if (chartTypeStr == null || columns == null || data == null)
            {
                return error("参数不完整");
            }

            // 转换图表类型字符串为枚举
            IChartRecommendService.ChartType chartType = null;
            for (IChartRecommendService.ChartType type : IChartRecommendService.ChartType.values())
            {
                if (type.getCode().equals(chartTypeStr))
                {
                    chartType = type;
                    break;
                }
            }
            
            if (chartType == null)
            {
                return error("不支持的图表类型: " + chartTypeStr);
            }
            
            Map<String, Object> config = chartConfigService.generateChartConfig(chartType, columns, data, columnDisplayFormats);

            return success(config);
        }
        catch (Exception e)
        {
            log.error("生成图表配置失败", e);
            return error("生成图表配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取查询建议接口
     */
    @PreAuthorize("@ss.hasPermi('query:suggest')")
    @GetMapping("/suggest")
    public AjaxResult getQuerySuggestions(
        @RequestParam("text") String text,
        @RequestParam(required = false, defaultValue = "10") Integer limit)
    {
        try
        {
            if (StringUtils.isEmpty(text))
            {
                return error("查询文本不能为空");
            }
            
            Long userId = SecurityUtils.getUserId();
            
            if (querySuggestService == null)
            {
                return success(Collections.emptyList());
            }
            
            List<IQuerySuggestService.QuerySuggestion> suggestions = 
                querySuggestService.getSuggestions(text, limit, userId);
            
            return success(suggestions);
        }
        catch (Exception e)
        {
            log.error("获取查询建议失败", e);
            return error("获取查询建议失败: " + e.getMessage());
        }
    }
    
    /**
     * 提交反馈接口
     */
    @PreAuthorize("@ss.hasPermi('query:feedback')")
    @Log(title = "查询反馈", businessType = BusinessType.INSERT)
    @PostMapping("/feedback")
    public AjaxResult submitFeedback(@RequestBody Feedback feedback)
    {
        try
        {
            if (feedback.getQueryId() == null)
            {
                return error("查询记录ID不能为空");
            }
            
            if (StringUtils.isEmpty(feedback.getFeedbackType()))
            {
                return error("反馈类型不能为空");
            }
            
            Long userId = SecurityUtils.getUserId();
            feedback.setUserId(userId);
            
            if (feedbackService == null)
            {
                return error("反馈服务未配置");
            }
            
            int result = feedbackService.submitFeedback(feedback);
            return toAjax(result);
        }
        catch (Exception e)
        {
            log.error("提交反馈失败", e);
            return error("提交反馈失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取反馈列表（数据管理员 bi:feedback:approve 也可查看）
     */
    @PreAuthorize("@ss.hasPermi('query:feedback:list') or @ss.hasPermi('bi:feedback:approve')")
    @GetMapping("/feedback/list")
    public TableDataInfo getFeedbackList(Feedback feedback)
    {
        startPage();
        
        // 普通用户只能查看自己的反馈，管理员可以查看所有
        Long userId = SecurityUtils.getUserId();
        if (!SecurityUtils.isAdmin(userId))
        {
            feedback.setUserId(userId);
        }
        
        if (feedbackService == null)
        {
            return getDataTable(Collections.emptyList());
        }
        
        List<Feedback> list = feedbackService.selectFeedbackList(feedback);
        return getDataTable(list);
    }
    
    /**
     * 审核反馈接口（管理员）
     */
    @PreAuthorize("@ss.hasPermi('query:feedback:review')")
    @Log(title = "查询反馈", businessType = BusinessType.UPDATE)
    @PutMapping("/feedback/review")
    public AjaxResult reviewFeedback(@RequestBody Map<String, Object> params)
    {
        try
        {
            Long id = ((Number) params.get("id")).longValue();
            String reviewStatus = (String) params.get("reviewStatus");
            String reviewComment = (String) params.get("reviewComment");
            
            if (id == null || StringUtils.isEmpty(reviewStatus))
            {
                return error("反馈ID和审核状态不能为空");
            }
            
            if (!"APPROVED".equals(reviewStatus) && !"REJECTED".equals(reviewStatus))
            {
                return error("审核状态必须是APPROVED或REJECTED");
            }
            
            if (feedbackService == null)
            {
                return error("反馈服务未配置");
            }
            
            String reviewer = SecurityUtils.getUsername();
            int result = feedbackService.reviewFeedback(id, reviewStatus, reviewComment, reviewer);
            
            return toAjax(result);
        }
        catch (Exception e)
        {
            log.error("审核反馈失败", e);
            return error("审核反馈失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询异步任务状态接口
     */
    @PreAuthorize("@ss.hasPermi('query:status')")
    @GetMapping("/task/status/{taskId}")
    public AjaxResult getTaskStatus(@PathVariable("taskId") Long taskId)
    {
        try
        {
            if (asyncQueryService == null)
            {
                return error("异步查询服务未配置");
            }
            
            AsyncQueryTask task = asyncQueryService.getTaskStatus(taskId);
            if (task == null)
            {
                return error("任务不存在");
            }
            
            // 检查权限（只能查看自己的任务）
            Long userId = SecurityUtils.getUserId();
            if (!task.getUserId().equals(userId) && !SecurityUtils.isAdmin(userId))
            {
                return error("无权访问该任务");
            }
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("taskId", task.getId());
            result.put("queryId", task.getQueryId());
            result.put("status", task.getStatus());
            result.put("progress", task.getProgress());
            result.put("errorMessage", task.getErrorMessage());
            result.put("startTime", task.getStartTime());
            result.put("endTime", task.getEndTime());
            result.put("executionTime", task.getExecutionTime());
            
            // 如果任务完成，返回查询结果
            if ("SUCCESS".equals(task.getStatus()) && queryRecordService != null)
            {
                QueryRecord queryRecord = queryRecordService.selectQueryRecordById(task.getQueryId());
                if (queryRecord != null && StringUtils.isNotEmpty(queryRecord.getResult()))
                {
                    try
                    {
                        List<Map<String, Object>> data = com.alibaba.fastjson2.JSON.parseObject(
                            queryRecord.getResult(),
                            new com.alibaba.fastjson2.TypeReference<List<Map<String, Object>>>() {}
                        );
                        result.put("data", data);
                        // 从结果数据中计算行数
                        result.put("rowCount", data != null ? data.size() : 0);
                        result.put("executionTime", queryRecord.getDuration());
                    }
                    catch (Exception e)
                    {
                        log.warn("解析查询结果失败: queryId={}", task.getQueryId(), e);
                    }
                }
            }
            
            return success(result);
        }
        catch (Exception e)
        {
            log.error("获取任务状态失败: taskId={}", taskId, e);
            return error("获取任务状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询异步任务状态接口（通过查询记录ID）
     */
    @PreAuthorize("@ss.hasPermi('query:status')")
    @GetMapping("/task/status/query/{queryId}")
    public AjaxResult getTaskStatusByQueryId(@PathVariable("queryId") Long queryId)
    {
        try
        {
            if (asyncQueryService == null)
            {
                return error("异步查询服务未配置");
            }
            
            AsyncQueryTask task = asyncQueryService.getTaskStatusByQueryId(queryId);
            if (task == null)
            {
                return error("任务不存在");
            }
            
            return getTaskStatus(task.getId());
        }
        catch (Exception e)
        {
            log.error("获取任务状态失败: queryId={}", queryId, e);
            return error("获取任务状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消异步查询任务接口
     */
    @PreAuthorize("@ss.hasPermi('query:cancel')")
    @PostMapping("/task/cancel/{taskId}")
    public AjaxResult cancelTask(@PathVariable("taskId") Long taskId)
    {
        try
        {
            if (asyncQueryService == null)
            {
                return error("异步查询服务未配置");
            }
            
            boolean result = asyncQueryService.cancelTask(taskId);
            if (result)
            {
                return success("任务已取消");
            }
            else
            {
                return error("取消任务失败，任务可能已完成或不存在");
            }
        }
        catch (Exception e)
        {
            log.error("取消任务失败: taskId={}", taskId, e);
            return error("取消任务失败: " + e.getMessage());
        }
    }
}
