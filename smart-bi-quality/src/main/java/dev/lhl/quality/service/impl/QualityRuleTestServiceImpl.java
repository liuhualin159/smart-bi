package dev.lhl.quality.service.impl;

import dev.lhl.datasource.service.IDataSourceService;
import dev.lhl.metadata.service.IMetadataService;
import dev.lhl.metadata.domain.TableMetadata;
import dev.lhl.quality.domain.BiQualityRule;
import dev.lhl.quality.domain.RuleExecutionResult;
import dev.lhl.quality.service.IQualityRuleEngine;
import dev.lhl.quality.service.IQualityRuleTestService;
import dev.lhl.quality.mapper.BiQualityRuleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

/**
 * 规则测试：抽样到临时表执行规则，测试后清理
 *
 * @author smart-bi
 */
@Service
public class QualityRuleTestServiceImpl implements IQualityRuleTestService {

    private static final Logger log = LoggerFactory.getLogger(QualityRuleTestServiceImpl.class);

    @Autowired
    private IDataSourceService dataSourceService;

    @Autowired(required = false)
    private IMetadataService metadataService;

    @Autowired
    private BiQualityRuleMapper biQualityRuleMapper;

    @Autowired
    private IQualityRuleEngine qualityRuleEngine;

    private static String escapeIdentifier(String s) {
        if (s == null || s.isEmpty()) return "";
        return "`" + s.replace("`", "``") + "`";
    }

    @Override
    public Map<String, Object> runRuleTest(Long tableId, int sampleSize) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tableId", tableId);
        result.put("sampleSize", sampleSize);
        result.put("passed", 0);
        result.put("failed", 0);
        result.put("totalRules", 0);
        result.put("results", new ArrayList<Map<String, Object>>());

        if (metadataService == null) {
            result.put("error", "元数据服务未配置");
            return result;
        }

        TableMetadata table = metadataService.selectTableMetadataById(tableId);
        if (table == null) {
            result.put("error", "表不存在");
            return result;
        }

        String tableName = table.getTableName();
        BiQualityRule query = new BiQualityRule();
        query.setTableId(tableId);
        query.setStatus("0");
        List<BiQualityRule> rules = biQualityRuleMapper.selectList(query);
        if (rules.isEmpty()) {
            result.put("message", "该表无已启用规则");
            return result;
        }

        String tempTable = "_bi_quality_sample_" + System.currentTimeMillis();
        Connection conn = null;

        try {
            conn = dataSourceService.getLocalConnection();

            // 1. 创建临时表并抽样
            createSampleTable(conn, tableName, tempTable, sampleSize);

            // 2. 对临时表执行规则
            List<Map<String, Object>> results = new ArrayList<>();
            int passed = 0, failed = 0;
            for (BiQualityRule rule : rules) {
                RuleExecutionResult res = qualityRuleEngine.executeRule(rule, tempTable);
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("ruleId", res.getRuleId());
                r.put("ruleType", res.getRuleType());
                r.put("passed", res.isPassed());
                r.put("totalRows", res.getTotalRows());
                r.put("failedRows", res.getFailedRows());
                r.put("message", res.getMessage());
                results.add(r);
                if (res.isPassed()) passed++; else failed++;
            }

            result.put("results", results);
            result.put("passed", passed);
            result.put("failed", failed);
            result.put("totalRules", rules.size());
        } catch (Exception e) {
            log.warn("规则测试异常: tableId={}", tableId, e);
            result.put("error", "执行异常: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    dropTable(conn, tempTable);
                } catch (Exception e) {
                    log.warn("删除临时表失败: {}", tempTable, e);
                }
                try {
                    conn.close();
                } catch (Exception ignored) {}
            }
        }

        return result;
    }

    /** 使用普通表而非 TEMPORARY，因规则引擎使用独立连接，MySQL 临时表仅对创建它的连接可见 */
    private void createSampleTable(Connection conn, String sourceTable, String tempTable, int sampleSize) throws SQLException {
        String sql = "CREATE TABLE " + escapeIdentifier(tempTable) +
            " AS SELECT * FROM " + escapeIdentifier(sourceTable) + " LIMIT " + Math.max(1, sampleSize);
        try (Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(120);
            stmt.executeUpdate(sql);
        }
    }

    private void dropTable(Connection conn, String tableName) throws SQLException {
        String sql = "DROP TABLE IF EXISTS " + escapeIdentifier(tableName);
        try (Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(30);
            stmt.executeUpdate(sql);
        }
    }
}
