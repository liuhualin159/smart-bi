package dev.lhl.metadata.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import dev.lhl.common.annotation.Excel;
import dev.lhl.common.core.domain.BaseEntity;

/**
 * 表元数据对象 bi_table_metadata
 * 
 * @author smart-bi
 */
public class TableMetadata extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 表元数据ID */
    private Long id;

    /** 表名 */
    @Excel(name = "表名")
    private String tableName;

    /** 表注释 */
    @Excel(name = "表注释")
    private String tableComment;

    /** 业务描述 */
    @Excel(name = "业务描述")
    private String businessDescription;

    /** 业务域ID */
    @Excel(name = "业务域ID")
    private Long domainId;

    /** 向量ID */
    private String vectorId;

    /** 表用途：PRIMARY/DIM/AGG/AUX/TEST/HIST */
    private String tableUsage;

    /** NL2SQL可见性：HIDDEN/NORMAL/PREFERRED */
    private String nl2sqlVisibilityLevel;

    /** 业务粒度描述 */
    private String grainDesc;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setTableName(String tableName)
    {
        this.tableName = tableName;
    }

    public String getTableName()
    {
        return tableName;
    }

    public void setTableComment(String tableComment)
    {
        this.tableComment = tableComment;
    }

    public String getTableComment()
    {
        return tableComment;
    }

    public void setBusinessDescription(String businessDescription)
    {
        this.businessDescription = businessDescription;
    }

    public String getBusinessDescription()
    {
        return businessDescription;
    }

    public void setDomainId(Long domainId)
    {
        this.domainId = domainId;
    }

    public Long getDomainId()
    {
        return domainId;
    }

    public void setVectorId(String vectorId)
    {
        this.vectorId = vectorId;
    }

    public String getVectorId()
    {
        return vectorId;
    }

    public void setTableUsage(String tableUsage) { this.tableUsage = tableUsage; }
    public String getTableUsage() { return tableUsage; }
    public void setNl2sqlVisibilityLevel(String nl2sqlVisibilityLevel) { this.nl2sqlVisibilityLevel = nl2sqlVisibilityLevel; }
    public String getNl2sqlVisibilityLevel() { return nl2sqlVisibilityLevel; }
    public void setGrainDesc(String grainDesc) { this.grainDesc = grainDesc; }
    public String getGrainDesc() { return grainDesc; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("tableName", getTableName())
            .append("tableComment", getTableComment())
            .append("businessDescription", getBusinessDescription())
            .append("domainId", getDomainId())
            .append("vectorId", getVectorId())
            .append("tableUsage", getTableUsage())
            .append("nl2sqlVisibilityLevel", getNl2sqlVisibilityLevel())
            .append("grainDesc", getGrainDesc())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
