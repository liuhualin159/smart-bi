package dev.lhl.etl.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import dev.lhl.common.annotation.Excel;

/**
 * ETL任务执行记录对象 bi_etl_task_execution
 * 
 * @author smart-bi
 */
public class EtlTaskExecution
{
    private static final long serialVersionUID = 1L;

    /** 执行记录ID */
    private Long id;

    /** 任务ID */
    @Excel(name = "任务ID")
    private Long taskId;

    /** 执行状态（RUNNING/SUCCESS/FAILED） */
    @Excel(name = "执行状态", readConverterExp = "RUNNING=运行中,SUCCESS=成功,FAILED=失败")
    private String status;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date endTime;

    /** 执行耗时（毫秒） */
    @Excel(name = "执行耗时(ms)")
    private Long duration;

    /** 抽取数据量 */
    @Excel(name = "数据量")
    private Long dataCount;

    /** 错误信息 */
    private String errorMessage;

    /** 断点信息（JSON格式） */
    private String checkpoint;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date createTime;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Long getTaskId()
    {
        return taskId;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStartTime(java.util.Date startTime)
    {
        this.startTime = startTime;
    }

    public java.util.Date getStartTime()
    {
        return startTime;
    }

    public void setEndTime(java.util.Date endTime)
    {
        this.endTime = endTime;
    }

    public java.util.Date getEndTime()
    {
        return endTime;
    }

    public void setDuration(Long duration)
    {
        this.duration = duration;
    }

    public Long getDuration()
    {
        return duration;
    }

    public void setDataCount(Long dataCount)
    {
        this.dataCount = dataCount;
    }

    public Long getDataCount()
    {
        return dataCount;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setCheckpoint(String checkpoint)
    {
        this.checkpoint = checkpoint;
    }

    public String getCheckpoint()
    {
        return checkpoint;
    }

    public void setCreateTime(java.util.Date createTime)
    {
        this.createTime = createTime;
    }

    public java.util.Date getCreateTime()
    {
        return createTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("taskId", getTaskId())
            .append("status", getStatus())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("duration", getDuration())
            .append("dataCount", getDataCount())
            .append("errorMessage", getErrorMessage())
            .append("createTime", getCreateTime())
            .toString();
    }
}
