package dev.lhl.metadata.domain;

import dev.lhl.common.core.domain.BaseEntity;

/**
 * 表推荐关系对象 bi_table_relation
 *
 * @author smart-bi
 */
public class TableRelation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;
    /** 左表名 */
    private String leftTable;
    /** 左表字段 */
    private String leftField;
    /** 右表名 */
    private String rightTable;
    /** 右表字段 */
    private String rightField;
    /** 关系类型 */
    private String relationType;
    /** 优先级 */
    private Integer priority;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLeftTable() { return leftTable; }
    public void setLeftTable(String leftTable) { this.leftTable = leftTable; }
    public String getLeftField() { return leftField; }
    public void setLeftField(String leftField) { this.leftField = leftField; }
    public String getRightTable() { return rightTable; }
    public void setRightTable(String rightTable) { this.rightTable = rightTable; }
    public String getRightField() { return rightField; }
    public void setRightField(String rightField) { this.rightField = rightField; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}
