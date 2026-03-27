package dev.lhl.query.service.impl;

import dev.lhl.common.utils.SqlSecurityUtils;
import dev.lhl.query.domain.QueryRecord;
import dev.lhl.query.service.IQueryDrillService;
import dev.lhl.query.service.IQueryExecutionService;
import dev.lhl.query.service.IQueryRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 下钻：在原 SQL 上追加 AND {dimension}={value}
 *
 * @author smart-bi
 */
@Service
public class QueryDrillServiceImpl implements IQueryDrillService {

    private static final Logger log = LoggerFactory.getLogger(QueryDrillServiceImpl.class);
    private static final Pattern SAFE_IDENT = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    @Autowired
    private IQueryRecordService queryRecordService;

    @Autowired
    private IQueryExecutionService queryExecutionService;

    @Autowired
    private PermissionInjectionService permissionInjectionService;

    @Override
    public DrillResult drill(Long queryId, String drillDimension, Object drillValue, Long userId) {
        if (queryId == null || drillDimension == null || drillDimension.trim().isEmpty()) {
            throw new RuntimeException("queryId 和 drillDimension 不能为空");
        }
        if (!SAFE_IDENT.matcher(drillDimension.trim()).matches()) {
            throw new RuntimeException("下钻维度字段名不合法");
        }
        QueryRecord record = queryRecordService.selectQueryRecordById(queryId);
        if (record == null) {
            throw new RuntimeException("查询记录不存在");
        }
        if (!record.getUserId().equals(userId) && !dev.lhl.common.utils.SecurityUtils.isAdmin(userId)) {
            throw new RuntimeException("无权访问该记录");
        }
        String baseSql = record.getExecutedSql();
        if (baseSql == null || baseSql.trim().isEmpty()) {
            baseSql = record.getGeneratedSql();
        }
        if (baseSql == null || baseSql.trim().isEmpty()) {
            throw new RuntimeException("原查询无可用 SQL");
        }
        String cond = buildDrillCondition(drillDimension.trim(), drillValue);
        String drilledSql = appendCondition(baseSql, cond);
        SqlSecurityUtils.validateSQL(drilledSql);
        String executedSql = permissionInjectionService.injectPermissions(drilledSql, userId);

        QueryRecord drillRecord = new QueryRecord();
        drillRecord.setSessionId(record.getSessionId());
        drillRecord.setUserId(userId);
        drillRecord.setQuestion(record.getQuestion() + " [下钻:" + drillDimension + "=" + drillValue + "]");
        drillRecord.setGeneratedSql(drilledSql);
        drillRecord.setExecutedSql(executedSql);
        drillRecord.setStatus("SUCCESS");

        IQueryExecutionService.QueryResult result = queryExecutionService.executeQuery(drillRecord, userId);
        return new DrillResult(drillRecord, result.getData(), result.getRowCount(), result.getExecutionTime());
    }

    private String buildDrillCondition(String dim, Object val) {
        if (val == null) return dim + " IS NULL";
        String v = String.valueOf(val).trim();
        if (v.isEmpty()) return dim + " = ''";
        v = v.replace("'", "''");
        return dim + " = '" + v + "'";
    }

    private String appendCondition(String sql, String cond) {
        String s = sql.trim();
        int orderIdx = upperIndexOf(s, " ORDER BY ");
        int groupIdx = upperIndexOf(s, " GROUP BY ");
        int insertPos = s.length();
        if (orderIdx > 0) insertPos = orderIdx;
        if (groupIdx > 0 && (insertPos == s.length() || groupIdx < insertPos)) insertPos = groupIdx;
        String before = s.substring(0, insertPos).trim();
        String after = insertPos < s.length() ? " " + s.substring(insertPos) : "";
        int whereIdx = upperIndexOf(before, " WHERE ");
        if (whereIdx > 0) {
            return before + " AND " + cond + after;
        }
        return before + " WHERE " + cond + after;
    }

    private static int upperIndexOf(String s, String sub) {
        int i = s.toUpperCase().indexOf(sub.toUpperCase());
        return i >= 0 ? i : -1;
    }
}
