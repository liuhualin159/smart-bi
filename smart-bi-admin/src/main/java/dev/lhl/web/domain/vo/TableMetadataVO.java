package dev.lhl.web.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.List;

/**
 * 表元数据列表 VO（含问题表统计）
 *
 * @author smart-bi
 */
public class TableMetadataVO
{
    private Long id;
    private String tableName;
    private String tableComment;
    private String businessDescription;
    private Long domainId;
    private String tableUsage;
    private String nl2sqlVisibilityLevel;
    private String grainDesc;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 统计时间窗口内该表相关错误次数 */
    private Integer errorCount;
    /** 最近一次错误时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastErrorTime;
    /** 最近错误类型 Top N（用于悬浮展示） */
    private List<String> lastErrorCategories;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getTableComment() { return tableComment; }
    public void setTableComment(String tableComment) { this.tableComment = tableComment; }
    public String getBusinessDescription() { return businessDescription; }
    public void setBusinessDescription(String businessDescription) { this.businessDescription = businessDescription; }
    public Long getDomainId() { return domainId; }
    public void setDomainId(Long domainId) { this.domainId = domainId; }
    public String getTableUsage() { return tableUsage; }
    public void setTableUsage(String tableUsage) { this.tableUsage = tableUsage; }
    public String getNl2sqlVisibilityLevel() { return nl2sqlVisibilityLevel; }
    public void setNl2sqlVisibilityLevel(String nl2sqlVisibilityLevel) { this.nl2sqlVisibilityLevel = nl2sqlVisibilityLevel; }
    public String getGrainDesc() { return grainDesc; }
    public void setGrainDesc(String grainDesc) { this.grainDesc = grainDesc; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public Integer getErrorCount() { return errorCount; }
    public void setErrorCount(Integer errorCount) { this.errorCount = errorCount; }
    public Date getLastErrorTime() { return lastErrorTime; }
    public void setLastErrorTime(Date lastErrorTime) { this.lastErrorTime = lastErrorTime; }
    public List<String> getLastErrorCategories() { return lastErrorCategories; }
    public void setLastErrorCategories(List<String> lastErrorCategories) { this.lastErrorCategories = lastErrorCategories; }
}
