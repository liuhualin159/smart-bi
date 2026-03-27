package dev.lhl.dashboard.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import dev.lhl.common.annotation.Excel;
import dev.lhl.common.core.domain.BaseEntity;

/**
 * 图表卡片对象 bi_chart_card
 * 
 * @author smart-bi
 */
public class ChartCard extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 卡片ID */
    private Long id;

    /** 卡片名称 */
    @Excel(name = "卡片名称")
    private String name;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 关联的查询记录ID */
    private Long queryId;

    /** 图表类型 */
    @Excel(name = "图表类型")
    private String chartType;

    /** 图表配置（ECharts option，JSON格式） */
    private String chartConfig;

    /** 关联的SQL */
    private String sql;

    /** 权限标签（JSON格式） */
    private String permissionTags;

    /** 最后刷新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后刷新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date lastRefreshTime;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }
    public void setName(String name) { this.name = name; }
    public String getName() { return name; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getUserId() { return userId; }
    public void setQueryId(Long queryId) { this.queryId = queryId; }
    public Long getQueryId() { return queryId; }
    public void setChartType(String chartType) { this.chartType = chartType; }
    public String getChartType() { return chartType; }
    public void setChartConfig(String chartConfig) { this.chartConfig = chartConfig; }
    public String getChartConfig() { return chartConfig; }
    public void setSql(String sql) { this.sql = sql; }
    public String getSql() { return sql; }
    public void setPermissionTags(String permissionTags) { this.permissionTags = permissionTags; }
    public String getPermissionTags() { return permissionTags; }
    public void setLastRefreshTime(java.util.Date lastRefreshTime) { this.lastRefreshTime = lastRefreshTime; }
    public java.util.Date getLastRefreshTime() { return lastRefreshTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("userId", getUserId())
            .append("chartType", getChartType())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
