package dev.lhl.quality.service;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 质量报告服务：生成报告（含根因分析）、导出 PDF/Excel、推送
 *
 * @author smart-bi
 */
public interface IQualityReportService {

    /**
     * 生成质量报告
     *
     * @param tableIds 表ID列表，为空则全部
     * @return 报告数据：表评分、规则结果、根因（失败规则及占比）
     */
    Map<String, Object> generateReport(List<Long> tableIds);

    /**
     * 导出为 Excel
     */
    void exportExcel(List<Long> tableIds, OutputStream out) throws Exception;

    /**
     * 推送报告到指定通道（邮件/钉钉/企业微信）
     * 需要外部配置，此处为占位接口
     */
    boolean pushReport(Map<String, Object> report, String channels) throws Exception;
}
