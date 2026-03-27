package dev.lhl.web.controller.export;

import dev.lhl.common.annotation.Log;
import dev.lhl.common.core.controller.BaseController;
import dev.lhl.common.enums.BusinessType;
import dev.lhl.dashboard.service.IExportService;
import dev.lhl.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 导出Controller
 * 负责导出图表和数据
 * 
 * @author smart-bi
 */
@RestController
@RequestMapping("/api/export")
public class ExportController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(ExportController.class);
    
    @Autowired
    private IExportService exportService;
    
    /**
     * 导出图表为PNG
     */
    @PreAuthorize("@ss.hasPermi('export:export')")
    @Log(title = "导出", businessType = BusinessType.EXPORT)
    @GetMapping("/chart/png/{cardId}")
    public void exportChartPng(@PathVariable("cardId") Long cardId, HttpServletResponse response)
    {
        try
        {
            log.info("导出图表PNG: cardId={}", cardId);
            
            response.setContentType("image/png");
            response.setHeader("Content-Disposition", 
                "attachment; filename=\"" + URLEncoder.encode("chart_" + cardId + ".png", StandardCharsets.UTF_8) + "\"");
            
            OutputStream outputStream = response.getOutputStream();
            exportService.exportChartAsPng(cardId, outputStream);
            outputStream.flush();
        }
        catch (Exception e)
        {
            log.error("导出图表PNG失败: cardId={}", cardId, e);
            try
            {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "导出失败: " + e.getMessage());
            }
            catch (Exception ex)
            {
                log.error("发送错误响应失败", ex);
            }
        }
    }
    
    /**
     * 导出图表为PDF
     */
    @PreAuthorize("@ss.hasPermi('export:export')")
    @Log(title = "导出", businessType = BusinessType.EXPORT)
    @GetMapping("/chart/pdf/{cardId}")
    public void exportChartPdf(@PathVariable("cardId") Long cardId, HttpServletResponse response)
    {
        try
        {
            log.info("导出图表PDF: cardId={}", cardId);
            
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", 
                "attachment; filename=\"" + URLEncoder.encode("chart_" + cardId + ".pdf", StandardCharsets.UTF_8) + "\"");
            
            OutputStream outputStream = response.getOutputStream();
            exportService.exportChartAsPdf(cardId, outputStream);
            outputStream.flush();
        }
        catch (Exception e)
        {
            log.error("导出图表PDF失败: cardId={}", cardId, e);
            try
            {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "导出失败: " + e.getMessage());
            }
            catch (Exception ex)
            {
                log.error("发送错误响应失败", ex);
            }
        }
    }
    
    /**
     * 按格式导出数据（支持 CSV、JSON、Parquet、Excel），支持脱敏
     */
    @PreAuthorize("@ss.hasPermi('export:export')")
    @Log(title = "导出", businessType = BusinessType.EXPORT)
    @PostMapping("/data")
    public void exportData(@RequestBody java.util.Map<String, Object> body, HttpServletResponse response)
    {
        try
        {
            Long queryId = body.get("queryId") != null ? ((Number) body.get("queryId")).longValue() : null;
            String sql = (String) body.get("sql");
            String format = (String) body.getOrDefault("format", "excel");
            Integer maxRows = body.get("maxRows") != null ? ((Number) body.get("maxRows")).intValue() : 10000;
            Boolean applyDesensitization = body.get("applyDesensitization") != null ? (Boolean) body.get("applyDesensitization") : true;

            if (queryId == null && (sql == null || sql.trim().isEmpty()))
            {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "queryId或sql参数不能为空");
                return;
            }

            Long userId = SecurityUtils.getUserId();
            IExportService.ExportFormat fmt = switch (format.toLowerCase()) {
                case "csv" -> IExportService.ExportFormat.CSV;
                case "json" -> IExportService.ExportFormat.JSON;
                case "parquet" -> IExportService.ExportFormat.PARQUET;
                default -> IExportService.ExportFormat.EXCEL;
            };

            String ext = switch (fmt) {
                case CSV -> "csv";
                case JSON -> "json";
                case PARQUET -> "parquet";
                case EXCEL -> "xlsx";
            };
            String contentType = switch (fmt) {
                case CSV -> "text/csv;charset=UTF-8";
                case JSON -> "application/json;charset=UTF-8";
                case PARQUET -> "application/octet-stream";
                case EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            };

            response.setContentType(contentType);
            response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode("data_export." + ext, StandardCharsets.UTF_8) + "\"");

            OutputStream outputStream = response.getOutputStream();
            exportService.exportData(queryId, sql, userId, fmt, outputStream, maxRows, applyDesensitization);
            outputStream.flush();
        }
        catch (Exception e)
        {
            log.error("导出数据失败", e);
            try
            {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "导出失败: " + e.getMessage());
            }
            catch (Exception ex)
            {
                log.error("发送错误响应失败", ex);
            }
        }
    }

    /**
     * 导出数据为Excel（兼容旧接口，GET 参数）
     */
    @PreAuthorize("@ss.hasPermi('export:export')")
    @Log(title = "导出", businessType = BusinessType.EXPORT)
    @GetMapping("/data/excel")
    public void exportDataExcel(
        @RequestParam(required = false) Long queryId,
        @RequestParam(required = false) String sql,
        @RequestParam(required = false, defaultValue = "10000") Integer maxRows,
        HttpServletResponse response)
    {
        try
        {
            log.info("导出数据Excel: queryId={}, maxRows={}", queryId, maxRows);
            
            if (queryId == null && (sql == null || sql.trim().isEmpty()))
            {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "queryId或sql参数不能为空");
                return;
            }
            
            Long userId = SecurityUtils.getUserId();
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", 
                "attachment; filename=\"" + URLEncoder.encode("data_export.xlsx", StandardCharsets.UTF_8) + "\"");
            
            OutputStream outputStream = response.getOutputStream();
            exportService.exportDataAsExcel(queryId, sql, userId, outputStream, maxRows);
            outputStream.flush();
        }
        catch (Exception e)
        {
            log.error("导出数据Excel失败: queryId={}", queryId, e);
            try
            {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "导出失败: " + e.getMessage());
            }
            catch (Exception ex)
            {
                log.error("发送错误响应失败", ex);
            }
        }
    }
}
