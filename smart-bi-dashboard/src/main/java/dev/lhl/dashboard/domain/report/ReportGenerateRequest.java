package dev.lhl.dashboard.domain.report;

import java.util.List;

/**
 * 报表生成请求：自然语言描述 + 可选约束
 */
public class ReportGenerateRequest {

    /** 用户自然语言描述（必填） */
    private String prompt;

    /** 看板ID（可选，不传则仅返回卡片与布局由前端写入） */
    private Long dashboardId;

    /** 数据源ID（可选，限定在该数据源下生成） */
    private Long datasourceId;

    /** 限定表ID列表（可选，与 NL2SQL 白名单取交集） */
    private List<Long> restrictToTableIds;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Long getDashboardId() {
        return dashboardId;
    }

    public void setDashboardId(Long dashboardId) {
        this.dashboardId = dashboardId;
    }

    public Long getDatasourceId() {
        return datasourceId;
    }

    public void setDatasourceId(Long datasourceId) {
        this.datasourceId = datasourceId;
    }

    public List<Long> getRestrictToTableIds() {
        return restrictToTableIds;
    }

    public void setRestrictToTableIds(List<Long> restrictToTableIds) {
        this.restrictToTableIds = restrictToTableIds;
    }
}
