package dev.lhl.query.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import dev.lhl.common.annotation.Excel;
import dev.lhl.common.core.domain.BaseEntity;

/**
 * 查询反馈对象 bi_query_feedback
 * 
 * @author smart-bi
 */
public class Feedback extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 反馈ID */
    private Long id;

    /** 查询记录ID */
    @Excel(name = "查询记录ID")
    private Long queryId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 反馈类型（CORRECT/INCORRECT/SUGGESTION） */
    @Excel(name = "反馈类型", readConverterExp = "CORRECT=正确,INCORRECT=错误,SUGGESTION=建议")
    private String feedbackType;

    /** 反馈内容 */
    @Excel(name = "反馈内容")
    private String content;

    /** 建议的SQL（如果是INCORRECT类型） */
    private String suggestedSql;

    /** 审核状态（PENDING/APPROVED/REJECTED） */
    @Excel(name = "审核状态", readConverterExp = "PENDING=待审核,APPROVED=已通过,REJECTED=已拒绝")
    private String reviewStatus;

    /** 审核意见 */
    private String reviewComment;

    /** 审核人 */
    private String reviewer;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "审核时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date reviewTime;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }
    
    public void setQueryId(Long queryId) { this.queryId = queryId; }
    public Long getQueryId() { return queryId; }
    
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getUserId() { return userId; }
    
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }
    public String getFeedbackType() { return feedbackType; }
    
    public void setContent(String content) { this.content = content; }
    public String getContent() { return content; }
    
    public void setSuggestedSql(String suggestedSql) { this.suggestedSql = suggestedSql; }
    public String getSuggestedSql() { return suggestedSql; }
    
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getReviewStatus() { return reviewStatus; }
    
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
    public String getReviewComment() { return reviewComment; }
    
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }
    public String getReviewer() { return reviewer; }
    
    public void setReviewTime(java.util.Date reviewTime) { this.reviewTime = reviewTime; }
    public java.util.Date getReviewTime() { return reviewTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("queryId", getQueryId())
            .append("userId", getUserId())
            .append("feedbackType", getFeedbackType())
            .append("content", getContent())
            .append("reviewStatus", getReviewStatus())
            .append("createTime", getCreateTime())
            .toString();
    }
}
