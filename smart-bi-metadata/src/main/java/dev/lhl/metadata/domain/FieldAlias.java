package dev.lhl.metadata.domain;

import dev.lhl.common.core.domain.BaseEntity;

/**
 * 字段别名对象 bi_field_alias
 *
 * @author smart-bi
 */
public class FieldAlias extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;
    /** 字段元数据ID */
    private Long fieldId;
    /** 别名内容 */
    private String alias;
    /** 来源：HUMAN/AUTO_SUGGEST/INFERRED_FROM_SQL */
    private String source;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFieldId() { return fieldId; }
    public void setFieldId(Long fieldId) { this.fieldId = fieldId; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
