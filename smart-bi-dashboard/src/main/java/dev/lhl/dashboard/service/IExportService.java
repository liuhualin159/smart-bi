package dev.lhl.dashboard.service;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 导出服务接口
 * 负责导出图表和数据（支持 Excel、CSV、JSON、Parquet）
 * 
 * @author smart-bi
 */
public interface IExportService
{
    /** 导出格式 */
    enum ExportFormat { EXCEL, CSV, JSON, PARQUET }

    /**
     * 导出图表为PNG
     * 
     * @param cardId 卡片ID
     * @param outputStream 输出流
     * @throws Exception 导出失败时抛出异常
     */
    void exportChartAsPng(Long cardId, OutputStream outputStream) throws Exception;
    
    /**
     * 导出图表为PDF
     * 
     * @param cardId 卡片ID
     * @param outputStream 输出流
     * @throws Exception 导出失败时抛出异常
     */
    void exportChartAsPdf(Long cardId, OutputStream outputStream) throws Exception;
    
    /**
     * 导出数据为Excel
     */
    void exportDataAsExcel(Long queryId, String sql, Long userId, OutputStream outputStream, Integer maxRows) throws Exception;

    /**
     * 按格式导出数据（CSV/JSON/Parquet），支持脱敏
     *
     * @param format 导出格式
     * @param queryId 查询记录ID（可选）
     * @param sql SQL语句（可选）
     * @param userId 用户ID
     * @param outputStream 输出流
     * @param maxRows 最大行数
     * @param applyDesensitization 是否应用脱敏规则
     */
    void exportData(Long queryId, String sql, Long userId, ExportFormat format,
                    OutputStream outputStream, Integer maxRows, boolean applyDesensitization) throws Exception;
}
