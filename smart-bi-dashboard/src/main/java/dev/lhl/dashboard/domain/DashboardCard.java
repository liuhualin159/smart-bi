package dev.lhl.dashboard.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 看板卡片关联对象 bi_dashboard_card
 * 
 * @author smart-bi
 */
public class DashboardCard
{
    private static final long serialVersionUID = 1L;

    /** 关联ID */
    private Long id;

    /** 看板ID */
    private Long dashboardId;

    /** 卡片ID */
    private Long cardId;

    /** 位置X */
    private Integer positionX;

    /** 位置Y */
    private Integer positionY;

    /** 宽度 */
    private Integer width;

    /** 高度 */
    private Integer height;

    /** 排序 */
    private Integer sortOrder;

    /** 组件类型: chart/decoration/group/datasource */
    private String componentType = "chart";

    /** 样式配置JSON */
    private String styleConfig;

    /** 父组合ID */
    private Long parentId;

    /** 装饰组件子类型 */
    private String decorationType;

    /** 组件显示名称 */
    private String cardName;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }
    public void setDashboardId(Long dashboardId) { this.dashboardId = dashboardId; }
    public Long getDashboardId() { return dashboardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }
    public Long getCardId() { return cardId; }
    public void setPositionX(Integer positionX) { this.positionX = positionX; }
    public Integer getPositionX() { return positionX; }
    public void setPositionY(Integer positionY) { this.positionY = positionY; }
    public Integer getPositionY() { return positionY; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getWidth() { return width; }
    public void setHeight(Integer height) { this.height = height; }
    public Integer getHeight() { return height; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getSortOrder() { return sortOrder; }
    public void setComponentType(String componentType) { this.componentType = componentType; }
    public String getComponentType() { return componentType; }
    public void setStyleConfig(String styleConfig) { this.styleConfig = styleConfig; }
    public String getStyleConfig() { return styleConfig; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Long getParentId() { return parentId; }
    public void setDecorationType(String decorationType) { this.decorationType = decorationType; }
    public String getDecorationType() { return decorationType; }
    public void setCardName(String cardName) { this.cardName = cardName; }
    public String getCardName() { return cardName; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("dashboardId", getDashboardId())
            .append("cardId", getCardId())
            .append("positionX", getPositionX())
            .append("positionY", getPositionY())
            .append("width", getWidth())
            .append("height", getHeight())
            .append("componentType", getComponentType())
            .append("styleConfig", getStyleConfig())
            .append("parentId", getParentId())
            .append("decorationType", getDecorationType())
            .append("cardName", getCardName())
            .toString();
    }
}
