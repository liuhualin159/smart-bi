package dev.lhl.common.core.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import dev.lhl.common.core.domain.BaseEntity;

/**
 * 数据权限对象 bi_data_permission
 * 
 * @author smart-bi
 */
public class DataPermission extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 权限ID */
    private Long id;

    /** 用户ID（null表示角色权限） */
    private Long userId;

    /** 角色ID（null表示用户权限） */
    private Long roleId;

    /** 权限类型（TABLE/FIELD/ROW） */
    private String permissionType;

    /** 表名 */
    private String tableName;

    /** 字段名（字段级权限时） */
    private String fieldName;

    /** 权限操作（ALLOW/DENY） */
    private String operation;

    /** 行级过滤条件（ROW权限时，SQL WHERE子句） */
    private String rowFilter;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getUserId() { return userId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public Long getRoleId() { return roleId; }
    public void setPermissionType(String permissionType) { this.permissionType = permissionType; }
    public String getPermissionType() { return permissionType; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getTableName() { return tableName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getFieldName() { return fieldName; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getOperation() { return operation; }
    public void setRowFilter(String rowFilter) { this.rowFilter = rowFilter; }
    public String getRowFilter() { return rowFilter; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("roleId", getRoleId())
            .append("permissionType", getPermissionType())
            .append("tableName", getTableName())
            .append("fieldName", getFieldName())
            .append("operation", getOperation())
            .toString();
    }
}
