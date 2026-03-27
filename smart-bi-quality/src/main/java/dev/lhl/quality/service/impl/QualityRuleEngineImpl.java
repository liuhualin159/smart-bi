package dev.lhl.quality.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import dev.lhl.datasource.service.IDataSourceService;
import dev.lhl.quality.domain.BiQualityRule;
import dev.lhl.quality.domain.RuleExecutionResult;
import dev.lhl.quality.service.IQualityRuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 规则引擎实现：完整性/准确性/一致性/唯一性/及时性
 *
 * @author smart-bi
 */
@Service
public class QualityRuleEngineImpl implements IQualityRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(QualityRuleEngineImpl.class);

    @Autowired
    private IDataSourceService dataSourceService;

    private static String escapeIdentifier(String s) {
        if (s == null || s.isEmpty()) return "";
        return "`" + s.replace("`", "``") + "`";
    }

    @Override
    public RuleExecutionResult executeRule(BiQualityRule rule, String tableName) {
        if (rule == null || tableName == null || tableName.isEmpty()) {
            return RuleExecutionResult.fail(rule != null ? rule.getId() : null, null, 0, 0, "参数无效", null);
        }
        String type = rule.getRuleType();
        if (type == null) type = "";
        try {
            return switch (type.toUpperCase()) {
                case "COMPLETENESS" -> checkCompleteness(rule, tableName);
                case "ACCURACY" -> checkAccuracy(rule, tableName);
                case "CONSISTENCY" -> checkConsistency(rule, tableName);
                case "UNIQUENESS" -> checkUniqueness(rule, tableName);
                case "TIMELINESS" -> checkTimeliness(rule, tableName);
                default -> RuleExecutionResult.fail(rule.getId(), type, 0, 0, "不支持的规则类型: " + type, null);
            };
        } catch (Exception e) {
            log.warn("规则执行异常: ruleId={}, type={}", rule.getId(), type, e);
            return RuleExecutionResult.fail(rule.getId(), type, 0, 0, "执行异常: " + e.getMessage(), null);
        }
    }

    /** 完整性：字段非空、非空字符串；支持单字段 field 或多字段 fields */
    private RuleExecutionResult checkCompleteness(BiQualityRule rule, String tableName) throws Exception {
        JSONObject cfg = parseConfig(rule.getRuleConfig());
        List<String> fieldList = resolveFieldList(cfg);
        if (fieldList == null || fieldList.isEmpty()) {
            return RuleExecutionResult.fail(rule.getId(), "COMPLETENESS", 0, 0, "缺少 field 或 fields 配置", null);
        }
        StringBuilder failedCond = new StringBuilder();
        for (int i = 0; i < fieldList.size(); i++) {
            String col = escapeIdentifier(fieldList.get(i));
            if (i > 0) failedCond.append(" OR ");
            failedCond.append("(").append(col).append(" IS NULL OR TRIM(COALESCE(CAST(").append(col).append(" AS CHAR), '')) = '')");
        }
        String sql = "SELECT COUNT(*) AS total, SUM(CASE WHEN " + failedCond + " THEN 1 ELSE 0 END) AS failed FROM " + escapeIdentifier(tableName);
        return executeCountQuery(rule, "COMPLETENESS", sql, tableName);
    }

    /** 准确性：格式/范围校验 */
    private RuleExecutionResult checkAccuracy(BiQualityRule rule, String tableName) throws Exception {
        JSONObject cfg = parseConfig(rule.getRuleConfig());
        if (cfg == null) {
            return RuleExecutionResult.fail(rule.getId(), "ACCURACY", 0, 0, "缺少 rule_config", null);
        }
        String field = cfg.getString("field");
        if (field == null || field.isEmpty()) {
            return RuleExecutionResult.fail(rule.getId(), "ACCURACY", 0, 0, "缺少 field 配置", null);
        }
        String col = escapeIdentifier(field);
        String pattern = cfg.getString("pattern");
        Double minVal = cfg.getDouble("min");
        Double maxVal = cfg.getDouble("max");

        String sql;
        if (pattern != null && !pattern.isEmpty()) {
            sql = "SELECT COUNT(*) AS total, SUM(CASE WHEN " + col + " REGEXP " + quote(pattern) + " THEN 0 ELSE 1 END) AS failed FROM " + escapeIdentifier(tableName) + " WHERE " + col + " IS NOT NULL";
        } else if (minVal != null || maxVal != null) {
            StringBuilder validCond = new StringBuilder();
            if (minVal != null) validCond.append(col).append(" >= ").append(minVal);
            if (maxVal != null) validCond.append(validCond.length() > 0 ? " AND " : "").append(col).append(" <= ").append(maxVal);
            sql = "SELECT COUNT(*) AS total, SUM(CASE WHEN " + col + " IS NULL OR NOT (" + validCond + ") THEN 1 ELSE 0 END) AS failed FROM " + escapeIdentifier(tableName);
        } else {
            return RuleExecutionResult.fail(rule.getId(), "ACCURACY", 0, 0, "需配置 pattern 或 min/max", null);
        }
        return executeCountQuery(rule, "ACCURACY", sql, tableName);
    }

    /** 一致性：外键/引用校验，本表字段值需存在于参考表 */
    private RuleExecutionResult checkConsistency(BiQualityRule rule, String tableName) throws Exception {
        JSONObject cfg = parseConfig(rule.getRuleConfig());
        if (cfg == null) {
            return RuleExecutionResult.fail(rule.getId(), "CONSISTENCY", 0, 0, "缺少 rule_config", null);
        }
        String field = cfg.getString("field");
        String refTable = cfg.getString("refTable");
        String refField = cfg.getString("refField");
        if (field == null || refTable == null || refField == null) {
            return RuleExecutionResult.fail(rule.getId(), "CONSISTENCY", 0, 0, "需配置 field、refTable、refField", null);
        }
        String col = escapeIdentifier(field);
        String refT = escapeIdentifier(refTable);
        String refF = escapeIdentifier(refField);
        String sql = "SELECT COUNT(*) AS failed FROM " + escapeIdentifier(tableName) + " t LEFT JOIN " + refT + " r ON t." + col + " = r." + refF + " WHERE t." + col + " IS NOT NULL AND r." + refF + " IS NULL";
        long failed = executeSingleLong(sql);
        String totalSql = "SELECT COUNT(*) FROM " + escapeIdentifier(tableName) + " WHERE " + col + " IS NOT NULL";
        long total = executeSingleLong(totalSql);
        if (total == 0) {
            return RuleExecutionResult.pass(rule.getId(), "CONSISTENCY", 0);
        }
        return failed == 0
            ? RuleExecutionResult.pass(rule.getId(), "CONSISTENCY", total)
            : RuleExecutionResult.fail(rule.getId(), "CONSISTENCY", total, failed, failed + " 条违反一致性", null);
    }

    /** 唯一性：无重复；支持单字段 field 或联合唯一 fields */
    private RuleExecutionResult checkUniqueness(BiQualityRule rule, String tableName) throws Exception {
        JSONObject cfg = parseConfig(rule.getRuleConfig());
        List<String> fieldList = resolveFieldList(cfg);
        if (fieldList == null || fieldList.isEmpty()) {
            return RuleExecutionResult.fail(rule.getId(), "UNIQUENESS", 0, 0, "缺少 field 或 fields 配置", null);
        }
        String distinctExpr;
        if (fieldList.size() == 1) {
            distinctExpr = escapeIdentifier(fieldList.get(0));
        } else {
            List<String> parts = new ArrayList<>();
            for (String f : fieldList) {
                parts.add("COALESCE(CAST(" + escapeIdentifier(f) + " AS CHAR),'')");
            }
            distinctExpr = "CONCAT(" + String.join(", CHAR(1), ", parts) + ")";
        }
        String sql = "SELECT COUNT(*) AS total, COUNT(*) - COUNT(DISTINCT " + distinctExpr + ") AS failed FROM " + escapeIdentifier(tableName);
        return executeCountQuery(rule, "UNIQUENESS", sql, tableName);
    }

    /** 解析 field 或 fields，兼容单字段与多字段；fastjson2 可能将数组解析为 List 而非 JSONArray */
    private List<String> resolveFieldList(JSONObject cfg) {
        if (cfg == null) return null;
        Object fieldsObj = cfg.get("fields");
        if (fieldsObj != null) {
            List<String> list = new ArrayList<>();
            if (fieldsObj instanceof JSONArray) {
                JSONArray arr = (JSONArray) fieldsObj;
                for (int i = 0; i < arr.size(); i++) {
                    String v = arr.getString(i);
                    if (v != null && !v.trim().isEmpty()) list.add(v.trim());
                }
            } else if (fieldsObj instanceof Collection) {
                for (Object o : (Collection<?>) fieldsObj) {
                    String v = o != null ? String.valueOf(o).trim() : "";
                    if (!v.isEmpty()) list.add(v);
                }
            }
            if (!list.isEmpty()) return list;
        }
        String field = cfg.getString("field");
        if (field != null && !field.trim().isEmpty()) {
            List<String> list = new ArrayList<>();
            list.add(field.trim());
            return list;
        }
        return null;
    }

    /** 及时性：数据新鲜度 */
    private RuleExecutionResult checkTimeliness(BiQualityRule rule, String tableName) throws Exception {
        JSONObject cfg = parseConfig(rule.getRuleConfig());
        if (cfg == null) {
            return RuleExecutionResult.fail(rule.getId(), "TIMELINESS", 0, 0, "缺少 rule_config", null);
        }
        String field = cfg.getString("field");
        Object maxAgeObj = cfg.get("maxAgeHours");
        Integer maxAgeHours = maxAgeObj != null ? (maxAgeObj instanceof Number ? ((Number) maxAgeObj).intValue() : Integer.parseInt(String.valueOf(maxAgeObj))) : null;
        if (field == null || maxAgeHours == null) {
            return RuleExecutionResult.fail(rule.getId(), "TIMELINESS", 0, 0, "需配置 field、maxAgeHours", null);
        }
        String col = escapeIdentifier(field);
        int hours = maxAgeHours;
        String sql = "SELECT COUNT(*) AS total, SUM(CASE WHEN " + col + " >= DATE_SUB(NOW(), INTERVAL " + hours + " HOUR) THEN 0 ELSE 1 END) AS failed FROM " + escapeIdentifier(tableName);
        return executeCountQuery(rule, "TIMELINESS", sql, tableName);
    }

    private JSONObject parseConfig(String config) {
        if (config == null || config.isEmpty()) return null;
        try {
            return JSON.parseObject(config);
        } catch (Exception e) {
            return null;
        }
    }

    private String quote(String s) {
        if (s == null) return "NULL";
        return "'" + s.replace("'", "''") + "'";
    }

    private RuleExecutionResult executeCountQuery(BiQualityRule rule, String type, String sql, String tableName) throws Exception {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSourceService.getLocalConnection();
            stmt = conn.createStatement();
            stmt.setQueryTimeout(60);
            rs = stmt.executeQuery(sql);
            long total = 0, failed = 0;
            if (rs.next()) {
                total = rs.getLong("total");
                failed = rs.getLong("failed");
            }
            if (total == 0) {
                return RuleExecutionResult.pass(rule.getId(), type, 0);
            }
            boolean passed = failed == 0;
            return passed
                ? RuleExecutionResult.pass(rule.getId(), type, total)
                : RuleExecutionResult.fail(rule.getId(), type, total, failed, failed + " 条未通过", null);
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (stmt != null) try { stmt.close(); } catch (Exception ignored) {}
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
        }
    }

    private long executeSingleLong(String sql) throws Exception {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSourceService.getLocalConnection();
            stmt = conn.createStatement();
            stmt.setQueryTimeout(60);
            rs = stmt.executeQuery(sql);
            return rs.next() ? rs.getLong(1) : 0;
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (stmt != null) try { stmt.close(); } catch (Exception ignored) {}
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
        }
    }
}
