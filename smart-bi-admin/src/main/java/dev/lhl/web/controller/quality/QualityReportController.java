package dev.lhl.web.controller.quality;

import dev.lhl.common.annotation.Log;
import dev.lhl.common.core.controller.BaseController;
import dev.lhl.common.core.domain.AjaxResult;
import dev.lhl.common.enums.BusinessType;
import dev.lhl.quality.service.IQualityReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 质量报告 Controller
 *
 * @author smart-bi
 */
@RestController
@RequestMapping("/api/quality/report")
public class QualityReportController extends BaseController {

    @Autowired(required = false)
    private IQualityReportService qualityReportService;

    @PreAuthorize("@ss.hasPermi('bi:quality:list')")
    @PostMapping("/generate")
    public AjaxResult generate(@RequestBody(required = false) Map<String, Object> params) {
        if (qualityReportService == null) return error("报告服务未配置");
        @SuppressWarnings("unchecked")
        List<Object> ids = params != null && params.containsKey("tableIds") ? (List<Object>) params.get("tableIds") : null;
        List<Long> tableIds = ids != null ? ids.stream().map(o -> Long.valueOf(String.valueOf(o))).collect(Collectors.toList()) : Collections.emptyList();
        Map<String, Object> report = qualityReportService.generateReport(tableIds.isEmpty() ? null : tableIds);
        return success(report);
    }

    @PreAuthorize("@ss.hasPermi('bi:quality:list')")
    @Log(title = "导出质量报告", businessType = BusinessType.EXPORT)
    @PostMapping("/export/excel")
    public void exportExcel(@RequestBody(required = false) Map<String, Object> params, HttpServletResponse response) {
        try {
            if (qualityReportService == null) {
                response.sendError(500, "报告服务未配置");
                return;
            }
            @SuppressWarnings("unchecked")
            List<Object> ids = params != null && params.containsKey("tableIds") ? (List<Object>) params.get("tableIds") : null;
            List<Long> tableIds = ids != null ? ids.stream().map(o -> Long.valueOf(String.valueOf(o))).collect(Collectors.toList()) : Collections.emptyList();

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode("quality_report.xlsx", StandardCharsets.UTF_8) + "\"");

            OutputStream out = response.getOutputStream();
            qualityReportService.exportExcel(tableIds.isEmpty() ? null : tableIds, out);
            out.flush();
        } catch (Exception e) {
            logger.error("导出质量报告失败", e);
        }
    }
}
