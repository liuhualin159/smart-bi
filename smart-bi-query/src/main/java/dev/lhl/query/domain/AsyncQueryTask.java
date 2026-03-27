package dev.lhl.query.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import dev.lhl.common.annotation.Excel;
import dev.lhl.common.core.domain.BaseEntity;

/**
 * 异步查询任务对象 bi_async_query_task
 * 
 * @author smart-bi
 */
public class AsyncQueryTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long id;

    /** 查询记录ID */
    @Excel(name = "查询记录ID")
    private Long queryId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 任务状态（PENDING/RUNNING/SUCCESS/FAILED/CANCELLED） */
    @Excel(name = "任务状态", readConverterExp = "PENDING=等待中,RUNNING=执行中,SUCCESS=成功,FAILED=失败,CANCELLED=已取消")
    private String status;

    /** 进度百分比（0-100） */
    @Excel(name = "进度")
    private Integer progress;

    /** 错误信息 */
    private String errorMessage;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date startTime;

    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "完成时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date endTime;

    /** 执行时长（毫秒） */
    @Excel(name = "执行时长(ms)")
    private Long executionTime;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }
    
    public void setQueryId(Long queryId) { this.queryId = queryId; }
    public Long getQueryId() { return queryId; }
    
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getUserId() { return userId; }
    
    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }
    
    public void setProgress(Integer progress) { this.progress = progress; }
    public Integer getProgress() { return progress; }
    
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getErrorMessage() { return errorMessage; }
    
    public void setStartTime(java.util.Date startTime) { this.startTime = startTime; }
    public java.util.Date getStartTime() { return startTime; }
    
    public void setEndTime(java.util.Date endTime) { this.endTime = endTime; }
    public java.util.Date getEndTime() { return endTime; }
    
    public void setExecutionTime(Long executionTime) { this.executionTime = executionTime; }
    public Long getExecutionTime() { return executionTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("queryId", getQueryId())
            .append("userId", getUserId())
            .append("status", getStatus())
            .append("progress", getProgress())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .toString();
    }
}
