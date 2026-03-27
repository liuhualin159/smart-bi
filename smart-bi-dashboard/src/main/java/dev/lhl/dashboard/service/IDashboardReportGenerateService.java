package dev.lhl.dashboard.service;

import dev.lhl.dashboard.domain.report.ReportGenerateProgressCallback;
import dev.lhl.dashboard.domain.report.ReportGenerateRequest;
import dev.lhl.dashboard.domain.report.ReportGenerateResult;

/**
 * 看板报表生成服务：根据自然语言描述生成图表卡片与布局
 */
public interface IDashboardReportGenerateService {

    /**
     * 根据用户描述生成报表卡片与布局
     *
     * @param request 请求（prompt 必填，可选 dashboardId、datasourceId、restrictToTableIds）
     * @param userId  当前用户ID
     * @return 卡片列表与布局；失败时 result 中带 errorCode、errorMessage
     */
    ReportGenerateResult generateReport(ReportGenerateRequest request, Long userId);

    /**
     * 流式生成：通过回调实时推送进度与每张卡片，用于 SSE
     *
     * @param request  请求
     * @param userId   当前用户ID
     * @param callback 进度回调，可为 null（则退化为无回调）
     * @return 最终结果；若 callback 非 null 且过程有错误可返回 failure
     */
    ReportGenerateResult generateReport(ReportGenerateRequest request, Long userId, ReportGenerateProgressCallback callback);
}
