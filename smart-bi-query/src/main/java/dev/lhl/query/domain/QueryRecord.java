package dev.lhl.query.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import dev.lhl.common.annotation.Excel;

/**
 * 查询记录对象 bi_query_record
 * 
 * @author smart-bi
 */
public class QueryRecord
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long id;

    /** 会话ID */
    @Excel(name = "会话ID")
    private Long sessionId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 自然语言问题 */
    @Excel(name = "问题")
    private String question;

    /** 生成的SQL */
    private String generatedSql;

    /** 实际执行的SQL（含权限注入） */
    private String executedSql;

    /** 执行状态 */
    @Excel(name = "执行状态", readConverterExp = "SUCCESS=成功,FAILED=失败")
    private String status;

    /** 执行结果（JSON格式） */
    private String result;

    /** 执行耗时（毫秒） */
    @Excel(name = "执行耗时(ms)")
    private Long duration;

    /** 错误信息 */
    private String errorMessage;

    /** NL2SQL 置信度 0-1 */
    private Double confidence;
    /** 澄清问题列表 JSON */
    private String disambiguationQuestions;
    /** 涉及表名列表 JSON */
    private String involvedTables;

    /** LLM 审计记录 ID（用于自修正后更新 retry_count/final_sql，非持久化） */
    private Long auditId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date createTime;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getSessionId() { return sessionId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getUserId() { return userId; }
    public void setQuestion(String question) { this.question = question; }
    public String getQuestion() { return question; }
    public void setGeneratedSql(String generatedSql) { this.generatedSql = generatedSql; }
    public String getGeneratedSql() { return generatedSql; }
    public void setExecutedSql(String executedSql) { this.executedSql = executedSql; }
    public String getExecutedSql() { return executedSql; }
    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }
    public void setResult(String result) { this.result = result; }
    public String getResult() { return result; }
    public void setDuration(Long duration) { this.duration = duration; }
    public Long getDuration() { return duration; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getErrorMessage() { return errorMessage; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public Double getConfidence() { return confidence; }
    public void setDisambiguationQuestions(String disambiguationQuestions) { this.disambiguationQuestions = disambiguationQuestions; }
    public String getDisambiguationQuestions() { return disambiguationQuestions; }
    public void setInvolvedTables(String involvedTables) { this.involvedTables = involvedTables; }
    public String getInvolvedTables() { return involvedTables; }
    public void setAuditId(Long auditId) { this.auditId = auditId; }
    public Long getAuditId() { return auditId; }
    public void setCreateTime(java.util.Date createTime) { this.createTime = createTime; }
    public java.util.Date getCreateTime() { return createTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("sessionId", getSessionId())
            .append("userId", getUserId())
            .append("question", getQuestion())
            .append("status", getStatus())
            .append("duration", getDuration())
            .append("createTime", getCreateTime())
            .toString();
    }
}
