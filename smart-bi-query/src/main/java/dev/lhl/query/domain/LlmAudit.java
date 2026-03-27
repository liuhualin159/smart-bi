package dev.lhl.query.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import dev.lhl.common.annotation.Excel;

/**
 * LLM审计对象 bi_llm_audit
 * 
 * @author smart-bi
 */
public class LlmAudit
{
    private static final long serialVersionUID = 1L;

    /** 审计ID */
    private Long id;

    /** 查询记录ID（对应 query_id） */
    @Excel(name = "查询记录ID")
    private Long queryRecordId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 原始问题（PII脱敏后，对应 original_question） */
    private String originalQuestion;

    /** 召回的表结构（对应 recalled_tables） */
    private String recalledTables;

    /** 生成的SQL（对应 generated_sql） */
    private String generatedSql;

    /** 模型名称 */
    @Excel(name = "模型名称")
    private String modelName;

    /** Token消耗（对应 token_usage，表为 text） */
    @Excel(name = "Token消耗")
    private String tokenUsage;

    /** 响应耗时（毫秒，对应 response_time） */
    @Excel(name = "请求耗时(ms)")
    private Long responseTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date createTime;

    /** 提示词版本 */
    private String promptVersion;
    /** 元数据schema版本 */
    private String metaSchemaVersion;
    /** 错误类型：WRONG_TABLE/WRONG_FIELD/WRONG_JOIN等 */
    private String errorCategory;
    /** 歧义优化状态：未处理/已处理 */
    private String processStatus;
    /** 自修正重试次数 */
    private Integer retryCount;
    /** 最终执行的SQL（修正后可能与generated_sql不同） */
    private String finalSql;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }
    public void setQueryRecordId(Long queryRecordId) { this.queryRecordId = queryRecordId; }
    public Long getQueryRecordId() { return queryRecordId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getUserId() { return userId; }
    public void setOriginalQuestion(String originalQuestion) { this.originalQuestion = originalQuestion; }
    public String getOriginalQuestion() { return originalQuestion; }
    public void setRecalledTables(String recalledTables) { this.recalledTables = recalledTables; }
    public String getRecalledTables() { return recalledTables; }
    public void setGeneratedSql(String generatedSql) { this.generatedSql = generatedSql; }
    public String getGeneratedSql() { return generatedSql; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getModelName() { return modelName; }
    public void setTokenUsage(String tokenUsage) { this.tokenUsage = tokenUsage; }
    public String getTokenUsage() { return tokenUsage; }
    public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }
    public Long getResponseTime() { return responseTime; }
    public void setCreateTime(java.util.Date createTime) { this.createTime = createTime; }
    public java.util.Date getCreateTime() { return createTime; }

    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public String getPromptVersion() { return promptVersion; }
    public void setMetaSchemaVersion(String metaSchemaVersion) { this.metaSchemaVersion = metaSchemaVersion; }
    public String getMetaSchemaVersion() { return metaSchemaVersion; }
    public void setErrorCategory(String errorCategory) { this.errorCategory = errorCategory; }
    public String getErrorCategory() { return errorCategory; }
    public void setProcessStatus(String processStatus) { this.processStatus = processStatus; }
    public String getProcessStatus() { return processStatus; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public Integer getRetryCount() { return retryCount; }
    public void setFinalSql(String finalSql) { this.finalSql = finalSql; }
    public String getFinalSql() { return finalSql; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("queryRecordId", getQueryRecordId())
            .append("userId", getUserId())
            .append("modelName", getModelName())
            .append("responseTime", getResponseTime())
            .append("createTime", getCreateTime())
            .toString();
    }
}
