package dev.lhl.metadata.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import dev.lhl.common.annotation.Excel;
import dev.lhl.common.core.domain.BaseEntity;

/**
 * 原子指标对象 bi_atomic_metric
 * 
 * @author smart-bi
 */
public class AtomicMetric extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 指标ID */
    private Long id;

    /** 指标名称 */
    @Excel(name = "指标名称")
    private String name;

    /** 指标编码 */
    @Excel(name = "指标编码")
    private String code;

    /** 指标表达式 */
    @Excel(name = "指标表达式")
    private String expression;

    /** 业务域ID */
    @Excel(name = "业务域ID")
    private Long domainId;

    /** 指标描述 */
    @Excel(name = "指标描述")
    private String description;

    /** 向量ID */
    private String vectorId;

    /** 同比/环比 SQL 模板或说明 */
    private String temporalExpression;

    /** 统计粒度说明 */
    private String metricGrain;
    /** 口径过滤前提 */
    private String metricFilter;

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

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getCode()
    {
        return code;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    public String getExpression()
    {
        return expression;
    }

    public void setDomainId(Long domainId)
    {
        this.domainId = domainId;
    }

    public Long getDomainId()
    {
        return domainId;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public void setVectorId(String vectorId)
    {
        this.vectorId = vectorId;
    }

    public String getVectorId()
    {
        return vectorId;
    }

    public void setTemporalExpression(String temporalExpression) { this.temporalExpression = temporalExpression; }
    public String getTemporalExpression() { return temporalExpression; }

    public void setMetricGrain(String metricGrain) { this.metricGrain = metricGrain; }
    public String getMetricGrain() { return metricGrain; }
    public void setMetricFilter(String metricFilter) { this.metricFilter = metricFilter; }
    public String getMetricFilter() { return metricFilter; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("code", getCode())
            .append("expression", getExpression())
            .append("domainId", getDomainId())
            .append("description", getDescription())
            .append("vectorId", getVectorId())
            .append("temporalExpression", getTemporalExpression())
            .append("metricGrain", getMetricGrain())
            .append("metricFilter", getMetricFilter())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
