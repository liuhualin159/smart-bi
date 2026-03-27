package dev.lhl.metadata.domain;

/**
 * 别名冲突项：同一 alias 出现在其他表/字段
 *
 * @author smart-bi
 */
public class AliasConflictItem {
    private Long tableId;
    private String tableName;
    private Long fieldId;
    private String fieldName;

    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public Long getFieldId() { return fieldId; }
    public void setFieldId(Long fieldId) { this.fieldId = fieldId; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
}
