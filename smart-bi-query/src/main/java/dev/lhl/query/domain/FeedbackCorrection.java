package dev.lhl.query.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.lhl.common.core.domain.BaseEntity;

/**
 * 反馈修正对象 bi_feedback_correction
 * 审核通过后用于 NL2SQL 相似问题优先采用
 *
 * @author smart-bi
 */
public class FeedbackCorrection extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;
    /** 关联查询记录ID */
    private Long queryId;
    /** 原始问题 */
    private String originalQuestion;
    /** 审核通过的正确SQL */
    private String correctedSql;
    /** 审核人(数据管理员) */
    private Long reviewedBy;
    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date reviewedAt;
    /** 状态：APPROVED/REJECTED */
    private String status;
    /** 是否已用于NL2SQL注入 */
    private Integer usedInNl2sql;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getQueryId() { return queryId; }
    public void setQueryId(Long queryId) { this.queryId = queryId; }
    public String getOriginalQuestion() { return originalQuestion; }
    public void setOriginalQuestion(String originalQuestion) { this.originalQuestion = originalQuestion; }
    public String getCorrectedSql() { return correctedSql; }
    public void setCorrectedSql(String correctedSql) { this.correctedSql = correctedSql; }
    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
    public java.util.Date getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(java.util.Date reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getUsedInNl2sql() { return usedInNl2sql; }
    public void setUsedInNl2sql(Integer usedInNl2sql) { this.usedInNl2sql = usedInNl2sql; }
}
