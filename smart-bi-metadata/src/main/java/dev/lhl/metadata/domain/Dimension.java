package dev.lhl.metadata.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import dev.lhl.common.annotation.Excel;
import dev.lhl.common.core.domain.BaseEntity;

/**
 * 维度对象 bi_dimension
 * 
 * @author smart-bi
 */
public class Dimension extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 维度ID */
    private Long id;

    /** 维度名称 */
    @Excel(name = "维度名称")
    private String name;

    /** 维度编码 */
    @Excel(name = "维度编码")
    private String code;

    /** 维度类型 */
    @Excel(name = "维度类型", readConverterExp = "TIME=时间,ORG=组织,PRODUCT=产品,CUSTOM=自定义")
    private String type;

    /** 关联字段名 */
    @Excel(name = "关联字段名")
    private String fieldName;

    /** 业务域ID */
    @Excel(name = "业务域ID")
    private Long domainId;

    /** 维度描述 */
    @Excel(name = "维度描述")
    private String description;

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

    public void setType(String type)
    {
        this.type = type;
    }

    public String getType()
    {
        return type;
    }

    public void setFieldName(String fieldName)
    {
        this.fieldName = fieldName;
    }

    public String getFieldName()
    {
        return fieldName;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("code", getCode())
            .append("type", getType())
            .append("fieldName", getFieldName())
            .append("domainId", getDomainId())
            .append("description", getDescription())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
