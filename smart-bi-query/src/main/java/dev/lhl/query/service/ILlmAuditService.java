package dev.lhl.query.service;

import dev.lhl.query.domain.LlmAudit;
import java.util.List;

/**
 * LLM请求审计服务接口
 * 负责记录LLM请求和响应，包括PII脱敏
 * 
 * @author smart-bi
 */
public interface ILlmAuditService
{
    /**
     * 记录LLM请求审计
     *
     * @param promptVersion 提示词版本（可选）
     * @param metaSchemaVersion 元数据 schema 版本（可选）
     * @param errorCategory 错误类型，成功时为 null（如 WRONG_TABLE/WRONG_FIELD/WRONG_JOIN）
     */
    Long recordAudit(Long userId, String originalQuestion, String recalledTables, String generatedSql, Long executionTime,
                     String promptVersion, String metaSchemaVersion, String errorCategory);

    /**
     * 记录LLM请求审计（含自修正信息）
     *
     * @param retryCount 自修正重试次数（0表示未重试）
     * @param finalSql 最终执行的SQL（修正后可能与generatedSql不同）
     */
    Long recordAudit(Long userId, String originalQuestion, String recalledTables, String generatedSql, Long executionTime,
                     String promptVersion, String metaSchemaVersion, String errorCategory, Integer retryCount, String finalSql);
    
    /**
     * 记录LLM请求审计（兼容旧调用，不写版本与错误类型）
     */
    Long recordAudit(Long userId, String originalQuestion, String recalledTables, String generatedSql, Long executionTime);
    
    /**
     * 更新审计记录的自修正信息（执行阶段重试后调用）
     *
     * @param auditId 审计记录ID
     * @param retryCount 实际重试次数
     * @param finalSql 最终执行的 SQL
     * @return 更新行数
     */
    int updateRetryInfo(Long auditId, int retryCount, String finalSql);
    
    /**
     * 对问题中的PII信息进行脱敏
     * 在发送给LLM前，需要脱敏手机号、身份证等敏感信息
     * 
     * @param question 原始问题
     * @return 脱敏后的问题
     */
    String desensitizePII(String question);
    
    /**
     * 查询审计记录
     * 
     * @param id 审计记录ID
     * @return 审计记录
     */
    LlmAudit selectLlmAuditById(Long id);

    /**
     * 查询时间窗口内有错误类型的审计记录（用于问题表统计）
     *
     * @param startTime 开始时间（含）
     * @return 审计记录列表
     */
    List<LlmAudit> listErrorsSince(java.util.Date startTime);

    /**
     * 歧义优化列表：分页由调用方通过 PageHelper.startPage 控制
     *
     * @param errorCategory 错误类型筛选（可选）
     * @param tableName 表名筛选，匹配 recalled_tables 或 generated_sql（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param processStatus 处理状态：PENDING=未处理，RESOLVED=已处理（可选）
     * @return 审计记录列表
     */
    List<LlmAudit> listForAmbiguity(String errorCategory, String tableName,
                                     java.util.Date startTime, java.util.Date endTime, String processStatus);

    /**
     * 歧义按表汇总：时间范围内有错误类型的记录，按表名与错误类型聚合（用于歧义页汇总视图）
     * 最多返回最近 3000 条记录参与聚合；未传时间时默认最近 90 天
     *
     * @param startTime 开始时间（可选，默认 90 天前）
     * @param endTime 结束时间（可选，默认当前）
     * @return 按表名聚合的统计，每项含 tableName、totalCount、categories（错误类型及次数）
     */
    List<dev.lhl.query.domain.dto.TableErrorSummary> getAmbiguitySummary(java.util.Date startTime, java.util.Date endTime);

    /**
     * 标记歧义记录为已处理（更新 process_status 为 RESOLVED）
     *
     * @param id 审计记录ID
     * @return 更新行数
     */
    int resolveAmbiguity(Long id);

    /**
     * 将审计记录绑定到查询记录（写回 bi_llm_audit.query_id）。
     * 用于「歧义优化」列表的「关联到问题」能力。
     *
     * @param auditId 审计记录ID
     * @param queryRecordId 查询记录ID
     * @return 更新行数
     */
    int bindQueryRecord(Long auditId, Long queryRecordId);
}
