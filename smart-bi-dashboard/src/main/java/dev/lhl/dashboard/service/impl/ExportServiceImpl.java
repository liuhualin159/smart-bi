package dev.lhl.dashboard.service.impl;

import dev.lhl.dashboard.domain.ChartCard;
import dev.lhl.dashboard.service.IChartCardService;
import dev.lhl.dashboard.service.IExportService;
import dev.lhl.query.domain.QueryRecord;
import dev.lhl.query.service.IQueryExecutionService;
import dev.lhl.query.service.IQueryRecordService;
import dev.lhl.query.service.impl.DesensitizeService;
import dev.lhl.common.utils.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 导出服务实现
 * 负责导出图表和数据
 * 
 * @author smart-bi
 */
@Service
public class ExportServiceImpl implements IExportService
{
    private static final Logger log = LoggerFactory.getLogger(ExportServiceImpl.class);
    
    // 最大导出行数
    private static final int DEFAULT_MAX_EXPORT_ROWS = 10000;
    
    @Autowired
    private IChartCardService chartCardService;
    
    @Autowired(required = false)
    private IQueryExecutionService queryExecutionService;
    
    @Autowired(required = false)
    private IQueryRecordService queryRecordService;

    @Autowired(required = false)
    private DesensitizeService desensitizeService;
    
    @Override
    public void exportChartAsPng(Long cardId, OutputStream outputStream) throws Exception
    {
        try
        {
            log.info("开始导出图表为PNG: cardId={}", cardId);
            
            ChartCard card = chartCardService.selectChartCardById(cardId);
            if (card == null)
            {
                throw new RuntimeException("卡片不存在: cardId=" + cardId);
            }
            
            // 获取图表配置
            String chartConfigJson = card.getChartConfig();
            if (StringUtils.isEmpty(chartConfigJson))
            {
                throw new RuntimeException("卡片没有图表配置");
            }
            
            // 注意：PNG导出建议在前端使用ECharts的getDataURL方法
            // 后端导出PNG使用Java标准库BufferedImage生成包含卡片信息的图片
            // 如需导出实际图表，建议使用headless浏览器（如Puppeteer）或在前端使用ECharts的getDataURL方法
            
            log.info("PNG导出：使用Java标准库生成包含卡片信息的图片。如需导出实际图表，建议使用前端ECharts的getDataURL方法。");
            
            // 生成一个包含卡片信息的简单PNG图片（白色背景，黑色文字）
            // 这是一个最小化的PNG实现，包含卡片名称和提示信息
            String cardName = card.getName() != null ? card.getName() : "未命名卡片";
            String message = "图表: " + cardName + "\n请使用前端导出功能";
            
            // 生成一个简单的PNG图片（200x100像素，白色背景，黑色文字）
            // 使用最小化的PNG格式
            byte[] pngImage = generateSimplePngWithText(cardName, message);
            
            outputStream.write(pngImage);
            outputStream.flush();
            
            log.info("图表PNG导出完成: cardId={}", cardId);
        }
        catch (Exception e)
        {
            log.error("导出图表PNG失败: cardId={}", cardId, e);
            throw new Exception("导出图表PNG失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void exportChartAsPdf(Long cardId, OutputStream outputStream) throws Exception
    {
        try
        {
            log.info("开始导出图表为PDF: cardId={}", cardId);
            
            ChartCard card = chartCardService.selectChartCardById(cardId);
            if (card == null)
            {
                throw new RuntimeException("卡片不存在: cardId=" + cardId);
            }
            
            // 注意：PDF导出使用Java标准库生成基本的PDF文档
            // 当前实现：生成一个包含卡片信息的PDF（纯文本格式，符合PDF标准）
            // 如需更丰富的PDF格式（如图表、样式等），建议集成PDF生成库（如iText、Apache PDFBox）
            
            log.info("PDF导出：使用Java标准库生成基本PDF文档。如需更丰富的格式，建议集成PDF生成库。");
            
            // 生成一个包含卡片信息的PDF
            String cardName = card.getName() != null ? card.getName() : "未命名卡片";
            String cardDescription = card.getChartType() != null ? "图表类型: " + card.getChartType() : "";
            String exportTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            
            // 使用 PDFBox 生成 PDF（支持中文）
            generatePdfWithPdfBox(cardName, cardDescription, exportTime, outputStream);
            
            log.info("图表PDF导出完成: cardId={}", cardId);
        }
        catch (Exception e)
        {
            log.error("导出图表PDF失败: cardId={}", cardId, e);
            throw new Exception("导出图表PDF失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void exportDataAsExcel(Long queryId, String sql, Long userId, OutputStream outputStream, Integer maxRows) throws Exception
    {
        log.info("开始导出数据为Excel: queryId={}, userId={}, maxRows={}", queryId, userId, maxRows);
        ExportDataResult result = fetchExportData(queryId, sql, userId, maxRows);
        writeExcel(result.columns, result.data, outputStream);
        log.info("数据Excel导出完成: queryId={}, rowCount={}", queryId, result.data.size());
    }

    private void writeExcel(List<String> columns, List<Map<String, Object>> data, OutputStream outputStream) throws Exception
    {
        Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("数据导出");
            
            // 创建样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++)
            {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(i));
                cell.setCellStyle(headerStyle);
            }
            
            // 创建数据行
            for (int i = 0; i < data.size(); i++)
            {
                Row row = sheet.createRow(i + 1);
                Map<String, Object> rowData = data.get(i);
                
                for (int j = 0; j < columns.size(); j++)
                {
                    Cell cell = row.createCell(j);
                    Object value = rowData.get(columns.get(j));
                    
                    if (value != null)
                    {
                        if (value instanceof Number)
                        {
                            cell.setCellValue(((Number) value).doubleValue());
                        }
                        else
                        {
                            cell.setCellValue(String.valueOf(value));
                        }
                    }
                    
                    cell.setCellStyle(dataStyle);
                }
            }
            
            // 自动调整列宽
            for (int i = 0; i < columns.size(); i++)
            {
                sheet.autoSizeColumn(i);
                // 设置最小列宽
                int columnWidth = sheet.getColumnWidth(i);
                if (columnWidth < 2000)
                {
                    sheet.setColumnWidth(i, 2000);
                }
            }
            
        workbook.write(outputStream);
        workbook.close();
    }

    @Override
    public void exportData(Long queryId, String sql, Long userId, IExportService.ExportFormat format,
                          OutputStream outputStream, Integer maxRows, boolean applyDesensitization) throws Exception {
        ExportDataResult result = fetchExportData(queryId, sql, userId, maxRows);
        List<Map<String, Object>> data = result.data;
        List<String> columns = result.columns;
        String tableName = result.tableName;

        if (applyDesensitization && desensitizeService != null && StringUtils.isNotEmpty(tableName)) {
            data = desensitizeService.desensitizeResults(data, tableName, userId);
        }

        switch (format) {
            case CSV -> writeCsv(columns, data, outputStream);
            case JSON -> writeJson(columns, data, outputStream);
            case PARQUET -> writeParquet(columns, data, outputStream);
            case EXCEL -> writeExcel(columns, data, outputStream);
            default -> throw new IllegalArgumentException("不支持的导出格式: " + format);
        }
    }

    private record ExportDataResult(List<Map<String, Object>> data, List<String> columns, String tableName) {}

    private ExportDataResult fetchExportData(Long queryId, String sql, Long userId, Integer maxRows) throws Exception {
        if (maxRows == null || maxRows <= 0) maxRows = DEFAULT_MAX_EXPORT_ROWS;
        List<Map<String, Object>> data = null;
        List<String> columns = null;
        String tableName = null;

        if (queryId != null && queryRecordService != null) {
            QueryRecord qr = queryRecordService.selectQueryRecordById(queryId);
            if (qr != null && StringUtils.isNotEmpty(qr.getResult())) {
                try {
                    List<Map<String, Object>> result = com.alibaba.fastjson2.JSON.parseObject(
                        qr.getResult(), new com.alibaba.fastjson2.TypeReference<List<Map<String, Object>>>() {});
                    if (result != null && !result.isEmpty()) {
                        data = result;
                        columns = new ArrayList<>(result.get(0).keySet());
                        if (StringUtils.isNotEmpty(qr.getInvolvedTables())) {
                            String tables = qr.getInvolvedTables();
                            if (tables.startsWith("[")) {
                                List<String> list = com.alibaba.fastjson2.JSON.parseArray(tables, String.class);
                                if (list != null && !list.isEmpty()) tableName = list.get(0);
                            } else {
                                tableName = tables.split(",")[0].trim().replace("\"", "");
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析查询结果失败: queryId={}", queryId, e);
                }
            }
        } else if (StringUtils.isNotEmpty(sql) && queryExecutionService != null) {
            QueryRecord qr = new QueryRecord();
            qr.setExecutedSql(sql);
            qr.setUserId(userId);
            IQueryExecutionService.QueryResult res = queryExecutionService.executeQuery(qr, userId);
            if (res.isSuccess() && res.getData() != null && !res.getData().isEmpty()) {
                data = res.getData();
                columns = new ArrayList<>(data.get(0).keySet());
                tableName = extractFirstTable(sql);
            }
        }

        if (data == null || data.isEmpty()) throw new RuntimeException("没有可导出的数据");
        if (data.size() > maxRows) data = data.subList(0, maxRows);
        return new ExportDataResult(data, columns, tableName);
    }

    private static String extractFirstTable(String sql) {
        if (sql == null) return null;
        String u = sql.toUpperCase();
        int i = u.indexOf(" FROM ");
        if (i < 0) return null;
        String rest = sql.substring(i + 6).trim();
        String[] parts = rest.split("\\s+");
        if (parts.length > 0) {
            String t = parts[0].replace("`", "");
            if (t.contains(".")) t = t.substring(t.lastIndexOf(".") + 1);
            return t;
        }
        return null;
    }

    private void writeCsv(List<String> columns, List<Map<String, Object>> data, OutputStream out) throws IOException {
        try (OutputStreamWriter w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            w.write('\uFEFF'); // BOM
            w.write(String.join(",", columns));
            w.write("\n");
            for (Map<String, Object> row : data) {
                List<String> vals = new ArrayList<>();
                for (String col : columns) {
                    Object v = row.get(col);
                    String s = v == null ? "" : escapeCsv(v.toString());
                    vals.add(s);
                }
                w.write(String.join(",", vals));
                w.write("\n");
            }
        }
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private void writeJson(List<String> columns, List<Map<String, Object>> data, OutputStream out) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : data) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (String col : columns) {
                m.put(col, row.get(col));
            }
            list.add(m);
        }
        byte[] bytes = com.alibaba.fastjson2.JSON.toJSONBytes(list, com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat);
        out.write(bytes);
    }

    private void writeParquet(List<String> columns, List<Map<String, Object>> data, OutputStream out) throws Exception {
        log.info("Parquet 格式暂以降级为 JSON 导出（Parquet 需额外依赖）");
        writeJson(columns, data, out);
    }
    
    /**
     * 生成简单的PNG图片（包含文本信息）
     * 使用Java标准库BufferedImage和Graphics2D生成真实的PNG图片
     */
    private byte[] generateSimplePngWithText(String title, String message)
    {
        try
        {
            // 创建800x600像素的图片
            int width = 800;
            int height = 600;
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
                width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
            
            // 获取Graphics2D对象
            java.awt.Graphics2D g2d = image.createGraphics();
            
            // 设置抗锯齿
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, 
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 填充白色背景
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, width, height);
            
            // 绘制边框
            g2d.setColor(java.awt.Color.LIGHT_GRAY);
            g2d.setStroke(new java.awt.BasicStroke(2));
            g2d.drawRect(10, 10, width - 20, height - 20);
            
            // 设置字体
            java.awt.Font titleFont = new java.awt.Font("Arial", java.awt.Font.BOLD, 24);
            java.awt.Font messageFont = new java.awt.Font("Arial", java.awt.Font.PLAIN, 16);
            
            // 绘制标题
            g2d.setColor(java.awt.Color.BLACK);
            g2d.setFont(titleFont);
            java.awt.FontMetrics titleMetrics = g2d.getFontMetrics(titleFont);
            int titleX = (width - titleMetrics.stringWidth(title)) / 2;
            int titleY = 80;
            g2d.drawString(title, titleX, titleY);
            
            // 绘制消息（支持多行）
            g2d.setFont(messageFont);
            java.awt.FontMetrics messageMetrics = g2d.getFontMetrics(messageFont);
            String[] lines = message.split("\n");
            int lineHeight = messageMetrics.getHeight();
            int startY = titleY + 60;
            
            for (int i = 0; i < lines.length; i++)
            {
                String line = lines[i];
                int lineX = (width - messageMetrics.stringWidth(line)) / 2;
                int lineY = startY + (i * lineHeight);
                g2d.drawString(line, lineX, lineY);
            }
            
            // 绘制提示信息
            String hint = "提示：建议使用前端ECharts的getDataURL方法导出图表";
            g2d.setColor(java.awt.Color.GRAY);
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.ITALIC, 12));
            java.awt.FontMetrics hintMetrics = g2d.getFontMetrics();
            int hintX = (width - hintMetrics.stringWidth(hint)) / 2;
            int hintY = height - 40;
            g2d.drawString(hint, hintX, hintY);
            
            // 释放资源
            g2d.dispose();
            
            // 将BufferedImage转换为PNG字节数组
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        }
        catch (Exception e)
        {
            log.error("生成PNG图片失败", e);
            // 如果生成失败，返回一个最小的有效PNG
            return new byte[]{
                (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, (byte)0x1F, 0x15, (byte)0xC4, (byte)0x89,
                0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54,
                0x78, 0x5E, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01,
                0x0D, (byte)0x0A, 0x2D, (byte)0xB4,
                0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte)0xAE, 0x42, 0x60, (byte)0x82
            };
        }
    }
    
    /**
     * 使用 PDFBox 生成 PDF（支持中文）
     * 尝试加载支持中文的字体，若无则使用 Helvetica（中文会显示为方块，但不会乱码）
     */
    private void generatePdfWithPdfBox(String title, String content, String exportTime, OutputStream outputStream) throws Exception
    {
        try (PDDocument document = new PDDocument())
        {
            PDPage page = new PDPage();
            document.addPage(page);

            org.apache.pdfbox.pdmodel.font.PDFont font = loadChineseFont(document);
            boolean isCjkFont = !(font instanceof PDType1Font);

            try (PDPageContentStream cs = new PDPageContentStream(document, page))
            {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;
                float leading = 18f;

                String fullText = ("标题: " + (title != null ? title : "") + "\n\n" +
                    "内容: " + (content != null ? content : "") + "\n\n" +
                    "导出时间: " + (exportTime != null ? exportTime : ""));

                if (!isCjkFont)
                {
                    fullText = fullText.replaceAll("[^\\x00-\\x7F]", "?");
                }

                cs.beginText();
                cs.setFont(font, 14);
                cs.newLineAtOffset(margin, y);
                cs.setLeading(leading);

                String[] lines = fullText.split("\n");
                for (String line : lines)
                {
                    if (line.length() > 80)
                    {
                        for (int i = 0; i < line.length(); i += 80)
                        {
                            int end = Math.min(i + 80, line.length());
                            cs.showText(line.substring(i, end));
                            cs.newLine();
                        }
                    }
                    else
                    {
                        cs.showText(line);
                        cs.newLine();
                    }
                }

                cs.endText();
            }

            document.save(outputStream);
        }
    }

    /**
     * 加载支持中文的字体：优先系统字体（Windows/Linux），再 classpath，否则回退到 Helvetica
     */
    private org.apache.pdfbox.pdmodel.font.PDFont loadChineseFont(PDDocument document)
    {
        String os = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = os.contains("win");

        String[] fontPaths = isWindows ? new String[] {
            "C:/Windows/Fonts/msyh.ttc",
            "C:/Windows/Fonts/simsun.ttc",
            "C:/Windows/Fonts/msyhbd.ttc",
            "fonts/NotoSansCJKsc-Regular.ttf",
            "fonts/simsun.ttf",
            "fonts/msyh.ttf"
        } : new String[] {
            "fonts/NotoSansCJKsc-Regular.ttf",
            "fonts/simsun.ttf",
            "fonts/msyh.ttf",
            System.getProperty("user.home") + "/.local/share/fonts/NotoSansCJKsc-Regular.ttf",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc"
        };

        for (String path : fontPaths)
        {
            try
            {
                InputStream is = null;
                if (path.startsWith("fonts/"))
                {
                    ClassPathResource resource = new ClassPathResource(path);
                    if (resource.exists())
                    {
                        is = resource.getInputStream();
                    }
                }
                else
                {
                    Path p = Paths.get(path);
                    if (Files.exists(p))
                    {
                        is = Files.newInputStream(p);
                    }
                }
                if (is != null)
                {
                    org.apache.pdfbox.pdmodel.font.PDFont font = PDType0Font.load(document, is);
                    log.debug("已加载中文字体: {}", path);
                    return font;
                }
            }
            catch (Exception e)
            {
                log.trace("无法加载字体 {}: {}", path, e.getMessage());
            }
        }

        log.warn("未找到中文字体，使用 Helvetica（中文可能显示异常）。可添加 fonts/NotoSansCJKsc-Regular.ttf 到 resources 目录");
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }
}
