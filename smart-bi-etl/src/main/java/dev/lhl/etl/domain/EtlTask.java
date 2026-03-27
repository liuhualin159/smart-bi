package dev.lhl.etl.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import dev.lhl.common.annotation.Excel;
import dev.lhl.common.core.domain.BaseEntity;

/**
 * ETL任务对象 bi_etl_task
 * 
 * @author smart-bi
 */
public class EtlTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long id;

    /** 任务名称 */
    @Excel(name = "任务名称")
    private String name;

    /** 数据源ID */
    @Excel(name = "数据源ID")
    private Long datasourceId;

    /** 源类型（TABLE/SQL/API） */
    @Excel(name = "源类型", readConverterExp = "TABLE=表,SQL=SQL查询,API=API接口")
    private String sourceType;

    /** 源配置（表名/SQL/API配置，JSON格式） */
    private String sourceConfig;

    /** 目标表名 */
    @Excel(name = "目标表名")
    private String targetTable;

    /** 抽取方式（FULL/INCREMENTAL） */
    @Excel(name = "抽取方式", readConverterExp = "FULL=全量,INCREMENTAL=增量")
    private String extractMode;

    /** 增量字段（时间戳/自增ID） */
    @Excel(name = "增量字段")
    private String incrementField;

    /** 增量类型（TIMESTAMP/AUTO_INCREMENT/CDC） */
    @Excel(name = "增量类型")
    private String incrementType;

    /** 调度类型（CRON/MANUAL） */
    @Excel(name = "调度类型", readConverterExp = "CRON=定时,MANUAL=手动")
    private String scheduleType;

    /** Cron表达式 */
    @Excel(name = "Cron表达式")
    private String cronExpression;

    /** 重试次数（默认3） */
    @Excel(name = "重试次数")
    private Integer retryCount;

    /** 重试间隔（JSON数组：[1,5,15]分钟） */
    private String retryInterval;

    /** 状态（ACTIVE/INACTIVE/PAUSED） */
    @Excel(name = "状态", readConverterExp = "ACTIVE=启用,INACTIVE=停用,PAUSED=暂停")
    private String status;

    /** 最后运行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后运行时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date lastRunTime;

    /** 下次运行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "下次运行时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date nextRunTime;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setDatasourceId(Long datasourceId)
    {
        this.datasourceId = datasourceId;
    }

    public Long getDatasourceId()
    {
        return datasourceId;
    }

    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceConfig(String sourceConfig)
    {
        this.sourceConfig = sourceConfig;
    }

    public String getSourceConfig()
    {
        return sourceConfig;
    }

    public void setTargetTable(String targetTable)
    {
        this.targetTable = targetTable;
    }

    public String getTargetTable()
    {
        return targetTable;
    }

    public void setExtractMode(String extractMode)
    {
        this.extractMode = extractMode;
    }

    public String getExtractMode()
    {
        return extractMode;
    }

    public void setIncrementField(String incrementField)
    {
        this.incrementField = incrementField;
    }

    public String getIncrementField()
    {
        return incrementField;
    }

    public void setIncrementType(String incrementType)
    {
        this.incrementType = incrementType;
    }

    public String getIncrementType()
    {
        return incrementType;
    }

    public void setScheduleType(String scheduleType)
    {
        this.scheduleType = scheduleType;
    }

    public String getScheduleType()
    {
        return scheduleType;
    }

    public void setCronExpression(String cronExpression)
    {
        this.cronExpression = cronExpression;
    }

    public String getCronExpression()
    {
        return cronExpression;
    }

    public void setRetryCount(Integer retryCount)
    {
        this.retryCount = retryCount;
    }

    public Integer getRetryCount()
    {
        return retryCount;
    }

    public void setRetryInterval(String retryInterval)
    {
        this.retryInterval = retryInterval;
    }

    public String getRetryInterval()
    {
        return retryInterval;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setLastRunTime(java.util.Date lastRunTime)
    {
        this.lastRunTime = lastRunTime;
    }

    public java.util.Date getLastRunTime()
    {
        return lastRunTime;
    }

    public void setNextRunTime(java.util.Date nextRunTime)
    {
        this.nextRunTime = nextRunTime;
    }

    public java.util.Date getNextRunTime()
    {
        return nextRunTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("datasourceId", getDatasourceId())
            .append("sourceType", getSourceType())
            .append("targetTable", getTargetTable())
            .append("extractMode", getExtractMode())
            .append("scheduleType", getScheduleType())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
