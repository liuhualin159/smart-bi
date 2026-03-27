package dev.lhl.dashboard.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import dev.lhl.common.annotation.Excel;
import dev.lhl.common.core.domain.BaseEntity;

/**
 * 看板对象 bi_dashboard
 * 
 * @author smart-bi
 */
public class Dashboard extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 看板ID */
    private Long id;

    /** 看板名称 */
    @Excel(name = "看板名称")
    private String name;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 布局配置（JSON格式） */
    private String layoutConfig;

    /** 刷新频率（分钟） */
    @Excel(name = "刷新频率(分钟)")
    private Integer refreshInterval;

    /** 是否公开 */
    @Excel(name = "是否公开", readConverterExp = "0=否,1=是")
    private Boolean isPublic;

    /** 背景配置（JSON格式） */
    private String backgroundConfig;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }
    public void setName(String name) { this.name = name; }
    public String getName() { return name; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getUserId() { return userId; }
    public void setLayoutConfig(String layoutConfig) { this.layoutConfig = layoutConfig; }
    public String getLayoutConfig() { return layoutConfig; }
    public void setRefreshInterval(Integer refreshInterval) { this.refreshInterval = refreshInterval; }
    public Integer getRefreshInterval() { return refreshInterval; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    public Boolean getIsPublic() { return isPublic; }
    public void setBackgroundConfig(String backgroundConfig) { this.backgroundConfig = backgroundConfig; }
    public String getBackgroundConfig() { return backgroundConfig; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("userId", getUserId())
            .append("refreshInterval", getRefreshInterval())
            .append("backgroundConfig", getBackgroundConfig())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
