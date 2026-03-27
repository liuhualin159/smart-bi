package dev.lhl.metadata.domain.dto;

import java.io.Serializable;

/**
 * 元数据自动补全项
 */
public class AutocompleteItem implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 类型：table / field / metric */
    private String type;
    /** 展示标签（表注释、字段名+别名、指标名） */
    private String label;
    /** 插入值（表名、字段名、指标名等） */
    private String value;

    public AutocompleteItem() {}

    public AutocompleteItem(String type, String label, String value)
    {
        this.type = type;
        this.label = label;
        this.value = value;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
