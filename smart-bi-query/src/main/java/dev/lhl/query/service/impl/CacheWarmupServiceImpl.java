package dev.lhl.query.service.impl;

import dev.lhl.datasource.service.IDataSourceService;
import dev.lhl.query.domain.QueryRecord;
import dev.lhl.query.mapper.QueryRecordMapper;
import dev.lhl.query.service.ICacheWarmupService;
import dev.lhl.query.service.IQueryCacheService;
import dev.lhl.query.util.CacheKeyUtil;
import dev.lhl.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

/**
 * 缓存预热服务实现
 * 基于最近成功查询记录，执行并缓存结果
 *
 * @author smart-bi
 */
@Service
public class CacheWarmupServiceImpl implements ICacheWarmupService {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmupServiceImpl.class);
    private static final int QUERY_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_TTL_SECONDS = 300;

    @Autowired(required = false)
    private IQueryCacheService queryCacheService;

    @Autowired(required = false)
    private IDataSourceService dataSourceService;

    @Autowired(required = false)
    private QueryRecordMapper queryRecordMapper;

    @Override
    public int warmupRecentQueries(int limit) {
        if (queryCacheService == null || dataSourceService == null || queryRecordMapper == null) {
            log.warn("缓存预热跳过：依赖服务未配置");
            return 0;
        }
        List<QueryRecord> records = queryRecordMapper.selectRecentSuccessRecords(Math.min(limit, 50));
        int warmed = 0;
        for (QueryRecord r : records) {
            String sql = r.getExecutedSql() != null ? r.getExecutedSql() : r.getGeneratedSql();
            if (StringUtils.isEmpty(sql)) continue;
            Long userId = r.getUserId();
            try {
                List<Map<String, Object>> data = executeQuery(sql);
                if (data != null && !data.isEmpty()) {
                    String key = CacheKeyUtil.generateCacheKey(sql, userId);
                    queryCacheService.cacheResult(key, data, DEFAULT_TTL_SECONDS);
                    warmed++;
                    log.debug("预热成功: queryRecordId={}, cacheKey={}", r.getId(), key);
                }
            } catch (Exception e) {
                log.warn("预热失败: queryRecordId={}, sql={}", r.getId(), sql.substring(0, Math.min(50, sql.length())) + "...", e);
            }
        }
        log.info("缓存预热完成: 尝试={}, 成功={}", records.size(), warmed);
        return warmed;
    }

    private List<Map<String, Object>> executeQuery(String sql) throws Exception {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSourceService.getLocalConnection();
            stmt = conn.createStatement();
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            rs = stmt.executeQuery(sql);
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            List<String> names = new ArrayList<>();
            for (int i = 1; i <= cols; i++) names.add(meta.getColumnLabel(i));
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) row.put(names.get(i - 1), rs.getObject(i));
                rows.add(row);
            }
            return rows;
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (stmt != null) try { stmt.close(); } catch (Exception ignored) {}
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
        }
    }
}
