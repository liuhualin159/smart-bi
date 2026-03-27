package dev.lhl.quality.service.impl;

import dev.lhl.metadata.domain.TableMetadata;
import dev.lhl.metadata.service.IMetadataService;
import dev.lhl.quality.domain.BiQualityRule;
import dev.lhl.quality.domain.BiQualityScore;
import dev.lhl.quality.domain.RuleExecutionResult;
import dev.lhl.quality.mapper.BiQualityRuleMapper;
import dev.lhl.quality.mapper.BiQualityScoreMapper;
import dev.lhl.quality.service.IQualityReportService;
import dev.lhl.quality.service.IQualityRuleEngine;
import dev.lhl.quality.service.IQualityScoreService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.*;

/**
 * 质量报告：汇总评分、规则结果、根因分析
 *
 * @author smart-bi
 */
@Service
public class QualityReportServiceImpl implements IQualityReportService {

    private static final Logger log = LoggerFactory.getLogger(QualityReportServiceImpl.class);

    @Autowired(required = false)
    private IMetadataService metadataService;

    @Autowired
    private BiQualityRuleMapper biQualityRuleMapper;

    @Autowired
    private BiQualityScoreMapper biQualityScoreMapper;

    @Autowired
    private IQualityRuleEngine qualityRuleEngine;

    @Autowired
    private IQualityScoreService qualityScoreService;

    private static final String[] RULE_TYPE_NAMES = {"COMPLETENESS", "ACCURACY", "CONSISTENCY", "UNIQUENESS", "TIMELINESS"};
    private static final Map<String, String> RULE_LABELS = Map.of(
        "COMPLETENESS", "完整性",
        "ACCURACY", "准确性",
        "CONSISTENCY", "一致性",
        "UNIQUENESS", "唯一性",
        "TIMELINESS", "及时性"
    );

    @Override
    public Map<String, Object> generateReport(List<Long> tableIds) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generatedAt", new Date());
        report.put("tables", new ArrayList<Map<String, Object>>());

        if (metadataService == null) return report;

        List<TableMetadata> tables;
        if (tableIds == null || tableIds.isEmpty()) {
            tables = metadataService.selectTableMetadataList(new TableMetadata());
        } else {
            tables = new ArrayList<>();
            for (Long id : tableIds) {
                TableMetadata t = metadataService.selectTableMetadataById(id);
                if (t != null) tables.add(t);
            }
        }

        for (TableMetadata t : tables) {
            Map<String, Object> tableReport = new LinkedHashMap<>();
            tableReport.put("tableId", t.getId());
            tableReport.put("tableName", t.getTableName());
            tableReport.put("tableComment", t.getTableComment());

            BiQualityScore latest = biQualityScoreMapper.selectLatestByTableAndType(t.getId(), "TABLE");
            tableReport.put("score", latest != null ? latest.getScore() : null);
            tableReport.put("calculatedAt", latest != null ? latest.getCalculatedAt() : null);

            BiQualityRule q = new BiQualityRule();
            q.setTableId(t.getId());
            q.setStatus("0");
            List<BiQualityRule> rules = biQualityRuleMapper.selectList(q);
            List<Map<String, Object>> ruleResults = new ArrayList<>();
            List<Map<String, Object>> rootCauses = new ArrayList<>();
            for (BiQualityRule rule : rules) {
                RuleExecutionResult res = qualityRuleEngine.executeRule(rule, t.getTableName());
                Map<String, Object> rr = new LinkedHashMap<>();
                rr.put("ruleId", rule.getId());
                rr.put("ruleType", rule.getRuleType());
                rr.put("ruleTypeLabel", RULE_LABELS.getOrDefault(rule.getRuleType(), rule.getRuleType()));
                rr.put("passed", res.isPassed());
                rr.put("totalRows", res.getTotalRows());
                rr.put("failedRows", res.getFailedRows());
                rr.put("message", res.getMessage());
                ruleResults.add(rr);
                if (!res.isPassed() && res.getTotalRows() > 0) {
                    Map<String, Object> rc = new LinkedHashMap<>(rr);
                    double failRate = (double) res.getFailedRows() / res.getTotalRows();
                    rc.put("failRate", String.format("%.1f%%", failRate * 100));
                    rootCauses.add(rc);
                }
            }
            tableReport.put("ruleResults", ruleResults);
            tableReport.put("rootCauses", rootCauses);

            ((List<Map<String, Object>>) report.get("tables")).add(tableReport);
        }

        return report;
    }

    @Override
    public void exportExcel(List<Long> tableIds, OutputStream out) throws Exception {
        Map<String, Object> report = generateReport(tableIds);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) report.get("tables");

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("质量报告");
        int rowNum = 0;

        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue("数据质量报告");
        titleRow.getCell(0).setCellStyle(headerStyle);
        rowNum++;

        for (Map<String, Object> tr : tables) {
            Row r = sheet.createRow(rowNum++);
            r.createCell(0).setCellValue("表名");
            r.createCell(1).setCellValue(String.valueOf(tr.get("tableName")));
            rowNum++;

            r = sheet.createRow(rowNum++);
            r.createCell(0).setCellValue("评分");
            r.createCell(1).setCellValue(tr.get("score") != null ? tr.get("score").toString() : "-");
            rowNum++;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ruleResults = (List<Map<String, Object>>) tr.get("ruleResults");
            if (ruleResults != null && !ruleResults.isEmpty()) {
                Row h = sheet.createRow(rowNum++);
                h.createCell(0).setCellValue("规则类型");
                h.createCell(1).setCellValue("结果");
                h.createCell(2).setCellValue("总行数");
                h.createCell(3).setCellValue("失败行数");
                h.createCell(4).setCellValue("说明");
                for (int i = 0; i <= 4; i++) h.getCell(i).setCellStyle(headerStyle);

                for (Map<String, Object> rr : ruleResults) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(String.valueOf(rr.get("ruleTypeLabel")));
                    row.createCell(1).setCellValue(Boolean.TRUE.equals(rr.get("passed")) ? "通过" : "未通过");
                    row.createCell(2).setCellValue(rr.get("totalRows") != null ? rr.get("totalRows").toString() : "");
                    row.createCell(3).setCellValue(rr.get("failedRows") != null ? rr.get("failedRows").toString() : "");
                    row.createCell(4).setCellValue(rr.get("message") != null ? String.valueOf(rr.get("message")) : "");
                }
                rowNum++;
            }
            rowNum++;
        }

        for (int i = 0; i < 5; i++) sheet.autoSizeColumn(i);
        wb.write(out);
        wb.close();
    }

    @Override
    public boolean pushReport(Map<String, Object> report, String channels) throws Exception {
        if (channels == null || channels.isEmpty()) return false;
        log.info("推送质量报告到: {}", channels);
        // 占位：实际需集成邮件、钉钉、企业微信等，此处仅日志
        return true;
    }
}
