package dev.lhl.dashboard.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import dev.lhl.common.core.domain.BaseEntity;

/**
 * 数据源卡片配置对象 bi_datasource_card_config
 * 
 * @author smart-bi
 */
public class DatasourceCardConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 配置ID */
    private Long id;

    /** 关联看板卡片ID */
    private Long dashboardCardId;

    /** 数据源ID */
    private Long datasourceId;

    /** SQL/API */
    private String queryType;

    /** SQL查询模板 */
    private String sqlTemplate;

    /** API地址 */
    private String apiUrl;

    /** HTTP方法 */
    private String apiMethod = "GET";

    /** 自定义Header JSON */
    private String apiHeaders;

    /** 请求体 */
    private String apiBody;

    /** 响应数据提取路径 */
    private String responseDataPath;

    /** 图表类型 */
    private String chartType;

    /** 图表配置覆盖 */
    private String chartConfigOverride;

    /** 列映射配置JSON */
    private String columnMapping;

    /** 刷新间隔 */
    private Integer refreshInterval = 0;

    /** 超时秒数 */
    private Integer queryTimeout = 30;

    /** 最大行数 */
    private Integer maxRows = 10000;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }
    public void setDashboardCardId(Long dashboardCardId) { this.dashboardCardId = dashboardCardId; }
    public Long getDashboardCardId() { return dashboardCardId; }
    public void setDatasourceId(Long datasourceId) { this.datasourceId = datasourceId; }
    public Long getDatasourceId() { return datasourceId; }
    public void setQueryType(String queryType) { this.queryType = queryType; }
    public String getQueryType() { return queryType; }
    public void setSqlTemplate(String sqlTemplate) { this.sqlTemplate = sqlTemplate; }
    public String getSqlTemplate() { return sqlTemplate; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getApiUrl() { return apiUrl; }
    public void setApiMethod(String apiMethod) { this.apiMethod = apiMethod; }
    public String getApiMethod() { return apiMethod; }
    public void setApiHeaders(String apiHeaders) { this.apiHeaders = apiHeaders; }
    public String getApiHeaders() { return apiHeaders; }
    public void setApiBody(String apiBody) { this.apiBody = apiBody; }
    public String getApiBody() { return apiBody; }
    public void setResponseDataPath(String responseDataPath) { this.responseDataPath = responseDataPath; }
    public String getResponseDataPath() { return responseDataPath; }
    public void setChartType(String chartType) { this.chartType = chartType; }
    public String getChartType() { return chartType; }
    public void setChartConfigOverride(String chartConfigOverride) { this.chartConfigOverride = chartConfigOverride; }
    public String getChartConfigOverride() { return chartConfigOverride; }
    public void setColumnMapping(String columnMapping) { this.columnMapping = columnMapping; }
    public String getColumnMapping() { return columnMapping; }
    public void setRefreshInterval(Integer refreshInterval) { this.refreshInterval = refreshInterval; }
    public Integer getRefreshInterval() { return refreshInterval; }
    public void setQueryTimeout(Integer queryTimeout) { this.queryTimeout = queryTimeout; }
    public Integer getQueryTimeout() { return queryTimeout; }
    public void setMaxRows(Integer maxRows) { this.maxRows = maxRows; }
    public Integer getMaxRows() { return maxRows; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("dashboardCardId", getDashboardCardId())
            .append("datasourceId", getDatasourceId())
            .append("queryType", getQueryType())
            .append("sqlTemplate", getSqlTemplate())
            .append("apiUrl", getApiUrl())
            .append("apiMethod", getApiMethod())
            .append("apiHeaders", getApiHeaders())
            .append("apiBody", getApiBody())
            .append("responseDataPath", getResponseDataPath())
            .append("chartType", getChartType())
            .append("chartConfigOverride", getChartConfigOverride())
            .append("columnMapping", getColumnMapping())
            .append("refreshInterval", getRefreshInterval())
            .append("queryTimeout", getQueryTimeout())
            .append("maxRows", getMaxRows())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
