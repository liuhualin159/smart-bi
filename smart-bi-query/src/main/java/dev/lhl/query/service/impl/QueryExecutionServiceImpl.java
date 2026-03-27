package dev.lhl.query.service.impl;

import dev.lhl.datasource.service.IDataSourceService;
import dev.lhl.query.domain.QueryRecord;
import dev.lhl.query.service.IQueryExecutionService;
import dev.lhl.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 查询执行服务实现
 * 负责执行SQL查询，包括COUNT探针检查、结果脱敏、错误处理等
 * 
 * @author smart-bi
 */
@Service
public class QueryExecutionServiceImpl implements IQueryExecutionService
{
    private static final Logger log = LoggerFactory.getLogger(QueryExecutionServiceImpl.class);
    
    // 查询超时时间：30秒
    private static final int QUERY_TIMEOUT_SECONDS = 30;
    
    // 最大返回行数：100万行
    private static final long MAX_RESULT_ROWS = 1_000_000L;
    
    @Autowired
    private IDataSourceService dataSourceService;
    
    @Autowired
    private DesensitizeService desensitizeService;
    
    @Autowired(required = false)
    private dev.lhl.query.service.IQueryCacheService queryCacheService;
    
    @Override
    public QueryResult executeQuery(QueryRecord queryRecord, Long userId)
    {
        long startTime = System.currentTimeMillis();
        
        try
        {
            log.info("开始执行查询: queryRecordId={}, userId={}, sql={}", 
                queryRecord.getId(), userId, queryRecord.getExecutedSql());
            
            String sql = queryRecord.getExecutedSql();
            if (StringUtils.isEmpty(sql))
            {
                sql = queryRecord.getGeneratedSql();
            }
            
            if (StringUtils.isEmpty(sql))
            {
                throw new RuntimeException("SQL语句为空");
            }
            
            // 1. COUNT探针检查
            long rowCount = countProbe(sql, userId);
            if (rowCount == 0)
            {
                log.info("COUNT探针检查：无数据，跳过完整查询执行");
                long executionTime = System.currentTimeMillis() - startTime;
                return QueryResult.success(Collections.emptyList(), 0, executionTime);
            }
            
            log.debug("COUNT探针检查：数据行数={}", rowCount);
            
            // 2. 检查结果集大小（如果超过限制，只返回部分数据）
            // 注意：COUNT探针可能返回-1（未知），这种情况下也执行查询
            if (rowCount > 0 && rowCount > MAX_RESULT_ROWS)
            {
                log.warn("查询结果过大: rowCount={}, maxRows={}", rowCount, MAX_RESULT_ROWS);
                // 添加LIMIT限制
                sql = addLimitClause(sql, MAX_RESULT_ROWS);
            }
            
            // 3. 检查缓存
            List<Map<String, Object>> results = null;
            if (queryCacheService != null)
            {
                String cacheKey = generateCacheKey(sql, userId);
                dev.lhl.query.service.IQueryCacheService.CachedResult cached = 
                    queryCacheService.getCachedResult(cacheKey);
                if (cached != null && !cached.isExpired())
                {
                    log.debug("从缓存获取查询结果: cacheKey={}", cacheKey);
                    results = cached.getData();
                }
            }
            
            // 4. 如果缓存未命中，执行查询
            if (results == null)
            {
                results = executeQueryWithTimeout(sql, userId);
                
                // 缓存结果（缓存5分钟）
                if (queryCacheService != null && results != null && !results.isEmpty())
                {
                    String cacheKey = generateCacheKey(sql, userId);
                    queryCacheService.cacheResult(cacheKey, results, 300);
                }
            }
            
            // 5. 提取表名用于脱敏
            String tableName = extractFirstTableName(sql);
            
            // 6. 脱敏处理
            if (tableName != null)
            {
                results = desensitizeService.desensitizeResults(results, tableName, userId);
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("查询执行成功: queryRecordId={}, rowCount={}, executionTime={}ms", 
                queryRecord.getId(), results.size(), executionTime);
            
            return QueryResult.success(results, results.size(), executionTime);
        }
        catch (TimeoutException e)
        {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("查询执行超时: queryRecordId={}, timeout={}s", queryRecord.getId(), QUERY_TIMEOUT_SECONDS);
            return QueryResult.failure("查询执行超时（超过" + QUERY_TIMEOUT_SECONDS + "秒）", executionTime, false);
        }
        catch (SQLException e)
        {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("查询执行失败: queryRecordId={}", queryRecord.getId(), e);
            return QueryResult.failure("数据库执行失败: " + e.getMessage(), executionTime, true);
        }
        catch (Exception e)
        {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("查询执行异常: queryRecordId={}", queryRecord.getId(), e);
            return QueryResult.failure("查询执行失败: " + e.getMessage(), executionTime, false);
        }
    }
    
    @Override
    public long countProbe(String sql, Long userId)
    {
        try
        {
            log.debug("执行COUNT探针检查: sql={}, userId={}", sql, userId);
            
            // 将SQL转换为COUNT查询
            String countSql = convertToCountSql(sql);
            
            // 执行COUNT查询
            Long count = executeCountQuery(countSql, userId);
            
            log.debug("COUNT探针检查结果: count={}", count);
            return count != null ? count : 0L;
        }
        catch (Exception e)
        {
            log.warn("COUNT探针检查失败，继续执行完整查询: sql={}", sql, e);
            // COUNT探针失败不影响主查询，返回-1表示未知
            return -1L;
        }
    }
    
    /**
     * 将SQL转换为COUNT查询
     */
    private String convertToCountSql(String sql)
    {
        if (StringUtils.isEmpty(sql))
        {
            return sql;
        }
        
        String upperSql = sql.toUpperCase().trim();
        
        // 若含 GROUP BY，查询会返回多行，第一列可能非数字，必须用子查询包装
        if (upperSql.contains(" GROUP BY "))
        {
            return "SELECT COUNT(*) FROM (" + sql + ") AS count_query";
        }
        
        // 若已是单行 COUNT 查询（无 GROUP BY），可直接使用
        if (upperSql.contains("COUNT("))
        {
            return sql;
        }
        
        // 提取FROM子句之后的部分
        int fromIndex = upperSql.indexOf(" FROM ");
        if (fromIndex < 0)
        {
            return "SELECT COUNT(*) FROM (" + sql + ") AS count_query";
        }
        
        // 构建COUNT查询
        String fromClause = sql.substring(fromIndex);
        
        // 移除ORDER BY、LIMIT等子句（COUNT查询不需要）
        fromClause = removeUnnecessaryClauses(fromClause);
        
        return "SELECT COUNT(*) " + fromClause;
    }
    
    /**
     * 移除不必要的子句（ORDER BY、LIMIT等）
     */
    private String removeUnnecessaryClauses(String sql)
    {
        String upperSql = sql.toUpperCase();
        
        // 移除ORDER BY
        int orderByIndex = upperSql.indexOf(" ORDER BY ");
        if (orderByIndex > 0)
        {
            sql = sql.substring(0, orderByIndex);
        }
        
        // 移除LIMIT
        int limitIndex = sql.toUpperCase().indexOf(" LIMIT ");
        if (limitIndex > 0)
        {
            sql = sql.substring(0, limitIndex);
        }
        
        return sql;
    }
    
    /**
     * 执行COUNT查询
     */
    private Long executeCountQuery(String countSql, Long userId) throws Exception
    {
        // 使用本地数据源执行查询
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try
        {
            conn = dataSourceService.getLocalConnection();
            stmt = conn.createStatement();
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            
            rs = stmt.executeQuery(countSql);
            if (rs.next())
            {
                return rs.getLong(1);
            }
            return 0L;
        }
        finally
        {
            if (rs != null) try { rs.close(); } catch (Exception e) { log.warn("关闭ResultSet失败", e); }
            if (stmt != null) try { stmt.close(); } catch (Exception e) { log.warn("关闭Statement失败", e); }
            if (conn != null) try { conn.close(); } catch (Exception e) { log.warn("关闭Connection失败", e); }
        }
    }
    
    /**
     * 执行查询（带超时控制）
     */
    private List<Map<String, Object>> executeQueryWithTimeout(String sql, Long userId) 
        throws SQLException, TimeoutException, InterruptedException, ExecutionException
    {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try
        {
            Future<List<Map<String, Object>>> future = executor.submit(() -> {
                return executeQueryInternal(sql, userId);
            });
            
            try
            {
                return future.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            catch (TimeoutException e)
            {
                future.cancel(true);
                throw new TimeoutException("查询执行超时");
            }
        }
        finally
        {
            executor.shutdown();
        }
    }
    
    /**
     * 执行查询（内部方法）
     */
    private List<Map<String, Object>> executeQueryInternal(String sql, Long userId) throws Exception
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try
        {
            conn = dataSourceService.getLocalConnection();
            stmt = conn.createStatement();
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            // 注意：使用 LIMIT 子句限制结果，而不是 setMaxRows
            
            // 注意：rowCount 在 executeQueryInternal 中不可用，LIMIT 限制已在 executeQuery 方法中处理
            rs = stmt.executeQuery(sql);
            
            // 获取结果集元数据
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // 提取列名
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++)
            {
                columnNames.add(metaData.getColumnLabel(i));
            }
            
            // 读取数据
            List<Map<String, Object>> results = new ArrayList<>();
            while (rs.next())
            {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++)
                {
                    String columnName = columnNames.get(i - 1);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
            
            log.debug("查询执行完成: rowCount={}", results.size());
            return results;
        }
        finally
        {
            if (rs != null) try { rs.close(); } catch (Exception e) { log.warn("关闭ResultSet失败", e); }
            if (stmt != null) try { stmt.close(); } catch (Exception e) { log.warn("关闭Statement失败", e); }
            if (conn != null) try { conn.close(); } catch (Exception e) { log.warn("关闭Connection失败", e); }
        }
    }
    
    /**
     * 提取SQL中的第一个表名
     */
    private String extractFirstTableName(String sql)
    {
        if (StringUtils.isEmpty(sql))
        {
            return null;
        }
        
        try
        {
            String upperSql = sql.toUpperCase();
            int fromIndex = upperSql.indexOf(" FROM ");
            if (fromIndex < 0)
            {
                return null;
            }
            
            String fromClause = sql.substring(fromIndex + 6).trim();
            // 提取表名（取第一个单词，支持表别名）
            // 注意：完整实现需要使用SQL解析器（如JSqlParser），当前实现适用于简单SELECT语句
            String[] parts = fromClause.split("\\s+");
            if (parts.length > 0)
            {
                String tableName = parts[0];
                // 移除反引号、数据库前缀等
                tableName = tableName.replace("`", "");
                if (tableName.contains("."))
                {
                    tableName = tableName.substring(tableName.lastIndexOf(".") + 1);
                }
                return tableName;
            }
        }
        catch (Exception e)
        {
            log.warn("提取表名失败: sql={}", sql, e);
        }
        
        return null;
    }
    
    /**
     * 生成缓存键（SQL的MD5哈希 + 用户ID）
     */
    private String generateCacheKey(String sql, Long userId)
    {
        try
        {
            String input = sql + "_" + userId;
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest)
            {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            log.warn("生成缓存键失败", e);
            return sql.hashCode() + "_" + userId;
        }
    }
    
    /**
     * 添加LIMIT子句
     */
    private String addLimitClause(String sql, long limit)
    {
        String upperSql = sql.toUpperCase();
        
        // 如果已有LIMIT，替换它
        int limitIndex = upperSql.indexOf(" LIMIT ");
        if (limitIndex > 0)
        {
            // 查找LIMIT子句的结束位置
            String limitClause = sql.substring(limitIndex);
            int spaceIndex = limitClause.indexOf(" ", 7);
            if (spaceIndex > 0)
            {
                return sql.substring(0, limitIndex) + " LIMIT " + limit;
            }
            else
            {
                return sql.substring(0, limitIndex) + " LIMIT " + limit;
            }
        }
        else
        {
            // 添加LIMIT子句
            return sql + " LIMIT " + limit;
        }
    }
}
