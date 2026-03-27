package dev.lhl.metadata.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import dev.lhl.common.annotation.Excel;
import dev.lhl.common.core.domain.BaseEntity;

/**
 * 字段元数据对象 bi_field_metadata
 * 
 * @author smart-bi
 */
public class FieldMetadata extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 字段元数据ID */
    private Long id;

    /** 表元数据ID */
    @Excel(name = "表ID")
    private Long tableId;

    /** 字段名 */
    @Excel(name = "字段名")
    private String fieldName;

    /** 字段类型 */
    @Excel(name = "字段类型")
    private String fieldType;

    /** 字段注释 */
    @Excel(name = "字段注释")
    private String fieldComment;

    /** 业务别名 */
    @Excel(name = "业务别名")
    private String businessAlias;

    /** 业务描述 */
    @Excel(name = "业务描述")
    private String businessDescription;

    /** 枚举值释义（JSON格式） */
    private String enumValues;

    /** 是否敏感字段 */
    @Excel(name = "是否敏感", readConverterExp = "0=否,1=是")
    private Boolean isSensitive;

    /** 脱敏规则 */
    private String desensitizeRule;

    /** 向量ID */
    private String vectorId;

    /** 用途：DIMENSION/MEASURE/OTHER */
    private String usageType;
    /** 语义类型 */
    private String semanticType;
    /** 单位 */
    private String unit;
    /** 默认聚合函数 */
    private String defaultAggFunc;
    /** 允许聚合函数列表(JSON或逗号分隔) */
    private String allowedAggFuncs;
    /** NL2SQL优先级1-10 */
    private Integer nl2sqlPriority;
    /** 敏感级别：LOW/MEDIUM/HIGH */
    private String sensitiveLevel;
    /** 曝光策略：MASK/AGG_ONLY/FORBIDDEN */
    private String exposurePolicy;
    /** 下钻路径JSON */
    private String drillPath;
    /** 显示格式JSON */
    private String displayFormat;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setTableId(Long tableId)
    {
        this.tableId = tableId;
    }

    public Long getTableId()
    {
        return tableId;
    }

    public void setFieldName(String fieldName)
    {
        this.fieldName = fieldName;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    public void setFieldType(String fieldType)
    {
        this.fieldType = fieldType;
    }

    public String getFieldType()
    {
        return fieldType;
    }

    public void setFieldComment(String fieldComment)
    {
        this.fieldComment = fieldComment;
    }

    public String getFieldComment()
    {
        return fieldComment;
    }

    public void setBusinessAlias(String businessAlias)
    {
        this.businessAlias = businessAlias;
    }

    public String getBusinessAlias()
    {
        return businessAlias;
    }

    public void setBusinessDescription(String businessDescription)
    {
        this.businessDescription = businessDescription;
    }

    public String getBusinessDescription()
    {
        return businessDescription;
    }

    public void setEnumValues(String enumValues)
    {
        this.enumValues = enumValues;
    }

    public String getEnumValues()
    {
        return enumValues;
    }

    public void setIsSensitive(Boolean isSensitive)
    {
        this.isSensitive = isSensitive;
    }

    public Boolean getIsSensitive()
    {
        return isSensitive;
    }

    public void setDesensitizeRule(String desensitizeRule)
    {
        this.desensitizeRule = desensitizeRule;
    }

    public String getDesensitizeRule()
    {
        return desensitizeRule;
    }

    public void setVectorId(String vectorId)
    {
        this.vectorId = vectorId;
    }

    public String getVectorId()
    {
        return vectorId;
    }

    public void setUsageType(String usageType) { this.usageType = usageType; }
    public String getUsageType() { return usageType; }
    public void setSemanticType(String semanticType) { this.semanticType = semanticType; }
    public String getSemanticType() { return semanticType; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getUnit() { return unit; }
    public void setDefaultAggFunc(String defaultAggFunc) { this.defaultAggFunc = defaultAggFunc; }
    public String getDefaultAggFunc() { return defaultAggFunc; }
    public void setAllowedAggFuncs(String allowedAggFuncs) { this.allowedAggFuncs = allowedAggFuncs; }
    public String getAllowedAggFuncs() { return allowedAggFuncs; }
    public void setNl2sqlPriority(Integer nl2sqlPriority) { this.nl2sqlPriority = nl2sqlPriority; }
    public Integer getNl2sqlPriority() { return nl2sqlPriority; }
    public void setSensitiveLevel(String sensitiveLevel) { this.sensitiveLevel = sensitiveLevel; }
    public String getSensitiveLevel() { return sensitiveLevel; }
    public void setExposurePolicy(String exposurePolicy) { this.exposurePolicy = exposurePolicy; }
    public String getExposurePolicy() { return exposurePolicy; }
    public void setDrillPath(String drillPath) { this.drillPath = drillPath; }
    public String getDrillPath() { return drillPath; }
    public void setDisplayFormat(String displayFormat) { this.displayFormat = displayFormat; }
    public String getDisplayFormat() { return displayFormat; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("tableId", getTableId())
            .append("fieldName", getFieldName())
            .append("fieldType", getFieldType())
            .append("fieldComment", getFieldComment())
            .append("businessAlias", getBusinessAlias())
            .append("businessDescription", getBusinessDescription())
            .append("isSensitive", getIsSensitive())
            .append("vectorId", getVectorId())
            .append("usageType", getUsageType())
            .append("semanticType", getSemanticType())
            .append("unit", getUnit())
            .append("defaultAggFunc", getDefaultAggFunc())
            .append("allowedAggFuncs", getAllowedAggFuncs())
            .append("nl2sqlPriority", getNl2sqlPriority())
            .append("sensitiveLevel", getSensitiveLevel())
            .append("exposurePolicy", getExposurePolicy())
            .append("drillPath", getDrillPath())
            .append("displayFormat", getDisplayFormat())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
