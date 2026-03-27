package dev.lhl.web.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.List;

/**
 * 字段元数据 VO（含别名列表）
 *
 * @author smart-bi
 */
public class FieldMetadataVO {

    private Long id;
    private Long tableId;
    private String fieldName;
    private String fieldType;
    private String fieldComment;
    private String businessAlias;
    private String businessDescription;
    private String usageType;
    private String semanticType;
    private String unit;
    private String defaultAggFunc;
    private String allowedAggFuncs;
    private Integer nl2sqlPriority;
    private String sensitiveLevel;
    private String exposurePolicy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 来自 bi_field_alias 的别名列表，含 source */
    private List<FieldAliasItem> aliases;

    public static class FieldAliasItem {
        private Long id;
        private String alias;
        private String source;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getAlias() { return alias; }
        public void setAlias(String alias) { this.alias = alias; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    public String getFieldComment() { return fieldComment; }
    public void setFieldComment(String fieldComment) { this.fieldComment = fieldComment; }
    public String getBusinessAlias() { return businessAlias; }
    public void setBusinessAlias(String businessAlias) { this.businessAlias = businessAlias; }
    public String getBusinessDescription() { return businessDescription; }
    public void setBusinessDescription(String businessDescription) { this.businessDescription = businessDescription; }
    public String getUsageType() { return usageType; }
    public void setUsageType(String usageType) { this.usageType = usageType; }
    public String getSemanticType() { return semanticType; }
    public void setSemanticType(String semanticType) { this.semanticType = semanticType; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getDefaultAggFunc() { return defaultAggFunc; }
    public void setDefaultAggFunc(String defaultAggFunc) { this.defaultAggFunc = defaultAggFunc; }
    public String getAllowedAggFuncs() { return allowedAggFuncs; }
    public void setAllowedAggFuncs(String allowedAggFuncs) { this.allowedAggFuncs = allowedAggFuncs; }
    public Integer getNl2sqlPriority() { return nl2sqlPriority; }
    public void setNl2sqlPriority(Integer nl2sqlPriority) { this.nl2sqlPriority = nl2sqlPriority; }
    public String getSensitiveLevel() { return sensitiveLevel; }
    public void setSensitiveLevel(String sensitiveLevel) { this.sensitiveLevel = sensitiveLevel; }
    public String getExposurePolicy() { return exposurePolicy; }
    public void setExposurePolicy(String exposurePolicy) { this.exposurePolicy = exposurePolicy; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public List<FieldAliasItem> getAliases() { return aliases; }
    public void setAliases(List<FieldAliasItem> aliases) { this.aliases = aliases; }
}
