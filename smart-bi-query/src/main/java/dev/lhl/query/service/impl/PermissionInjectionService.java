package dev.lhl.query.service.impl;

import dev.lhl.common.core.domain.entity.SysUser;
import dev.lhl.common.utils.SecurityUtils;
import dev.lhl.common.utils.StringUtils;
import dev.lhl.system.service.IDataPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 权限注入服务
 * 负责在SQL中注入表级、行级权限过滤条件
 * 
 * @author smart-bi
 */
@Service
public class PermissionInjectionService
{
    private static final Logger log = LoggerFactory.getLogger(PermissionInjectionService.class);
    
    @Autowired(required = false)
    private IDataPermissionService dataPermissionService;
    
    /**
     * 注入权限过滤条件到SQL中
     * 
     * @param sql 原始SQL
     * @param userId 用户ID
     * @return 注入权限后的SQL
     */
    public String injectPermissions(String sql, Long userId)
    {
        if (sql == null || sql.trim().isEmpty() || userId == null)
        {
            log.debug("SQL或用户ID为空，跳过权限注入");
            return sql;
        }
        
        try
        {
            log.debug("开始注入权限: userId={}, sql={}", userId, sql);
            
            // 1. 检查用户是否为管理员（管理员不需要权限过滤）
            SysUser user = SecurityUtils.getLoginUser().getUser();
            if (user != null && user.isAdmin())
            {
                log.debug("管理员用户，跳过权限注入");
                return sql;
            }
            
            // 2. 提取SQL中涉及的表名
            Set<String> tableNames = extractTableNames(sql);
            if (tableNames.isEmpty())
            {
                log.debug("未识别到表名，跳过权限注入");
                return sql;
            }
            
            log.debug("识别到表名: {}", tableNames);
            
            // 3. 检查表级权限，如果有DENY权限则拒绝
            if (dataPermissionService != null)
            {
                for (String tableName : tableNames)
                {
                    log.debug("检查表权限: userId={}, tableName={}", userId, tableName);
                    boolean hasTablePermission = dataPermissionService.checkTablePermission(userId, tableName);
                    if (!hasTablePermission)
                    {
                        log.warn("用户无表访问权限: userId={}, tableName={}", userId, tableName);
                        return "SELECT 1 FROM (SELECT 1) AS no_permission WHERE 1=0";
                    }
                }
            }
            else
            {
                log.warn("数据权限服务未配置，跳过表级权限检查");
            }
            
            // 4. 获取行级权限过滤条件
            List<String> rowFilters = new ArrayList<>();
            if (dataPermissionService != null)
            {
                for (String tableName : tableNames)
                {
                    String rowFilter = dataPermissionService.getRowFilter(userId, tableName);
                    if (StringUtils.isNotEmpty(rowFilter))
                    {
                        rowFilter = replacePlaceholders(rowFilter, userId, user);
                        rowFilters.add("(" + rowFilter + ")");
                        log.debug("获取到行级权限过滤条件: userId={}, tableName={}, filter={}", userId, tableName, rowFilter);
                    }
                }
            }
            else
            {
                log.warn("数据权限服务未配置，跳过行级权限过滤");
            }
            
            // 5. 如果有行级过滤条件，注入到SQL的WHERE子句中
            if (!rowFilters.isEmpty())
            {
                sql = injectRowFilters(sql, rowFilters);
                log.debug("已注入行级权限过滤条件: userId={}, filters={}", userId, rowFilters);
            }
            
            log.debug("权限注入完成: userId={}", userId);
            return sql;
        }
        catch (Exception e)
        {
            log.error("权限注入失败，返回原始SQL: userId={}", userId, e);
            // 权限注入失败时，为了安全起见，返回一个不返回任何结果的SQL
            return "SELECT 1 FROM (SELECT 1) AS permission_error WHERE 1=0";
        }
    }
    
    /**
     * 从SQL中提取表名
     * 支持 FROM table_name, JOIN table_name 等常见模式
     */
    private Set<String> extractTableNames(String sql)
    {
        Set<String> tableNames = new HashSet<>();
        
        if (StringUtils.isEmpty(sql))
        {
            return tableNames;
        }
        
        try
        {
            String upperSql = sql.toUpperCase().trim();
            
            // 匹配 FROM 子句中的表名
            Pattern fromPattern = Pattern.compile(
                "FROM\\s+([A-Z_][A-Z0-9_]*(?:\\.[A-Z_][A-Z0-9_]*)?)",
                Pattern.CASE_INSENSITIVE
            );
            Matcher fromMatcher = fromPattern.matcher(upperSql);
            while (fromMatcher.find())
            {
                String tableName = fromMatcher.group(1).toLowerCase();
                // 移除数据库名前缀（如果有）
                if (tableName.contains("."))
                {
                    tableName = tableName.substring(tableName.lastIndexOf(".") + 1);
                }
                // 移除反引号
                tableName = tableName.replace("`", "");
                tableNames.add(tableName);
            }
            
            // 匹配 JOIN 子句中的表名
            Pattern joinPattern = Pattern.compile(
                "(?:INNER|LEFT|RIGHT|FULL)?\\s+JOIN\\s+([A-Z_][A-Z0-9_]*(?:\\.[A-Z_][A-Z0-9_]*)?)",
                Pattern.CASE_INSENSITIVE
            );
            Matcher joinMatcher = joinPattern.matcher(upperSql);
            while (joinMatcher.find())
            {
                String tableName = joinMatcher.group(1).toLowerCase();
                if (tableName.contains("."))
                {
                    tableName = tableName.substring(tableName.lastIndexOf(".") + 1);
                }
                tableName = tableName.replace("`", "");
                tableNames.add(tableName);
            }
        }
        catch (Exception e)
        {
            log.warn("提取表名失败: sql={}", sql, e);
        }
        
        return tableNames;
    }
    
    /**
     * 替换占位符
     */
    private String replacePlaceholders(String rowFilter, Long userId, SysUser user)
    {
        if (StringUtils.isEmpty(rowFilter))
        {
            return rowFilter;
        }
        
        String result = rowFilter;
        
        // 替换 #{userId}
        result = result.replace("#{userId}", String.valueOf(userId));
        result = result.replace("${userId}", String.valueOf(userId));
        
        // 替换 #{deptId}
        if (user != null && user.getDeptId() != null)
        {
            result = result.replace("#{deptId}", String.valueOf(user.getDeptId()));
            result = result.replace("${deptId}", String.valueOf(user.getDeptId()));
        }
        
        return result;
    }
    
    /**
     * 将行级过滤条件注入到SQL的WHERE子句中
     */
    private String injectRowFilters(String sql, List<String> rowFilters)
    {
        if (rowFilters == null || rowFilters.isEmpty())
        {
            return sql;
        }
        
        try
        {
            // 合并所有过滤条件
            String combinedFilter = String.join(" AND ", rowFilters);
            
            // 检查SQL中是否已有WHERE子句
            String upperSql = sql.toUpperCase();
            int whereIndex = upperSql.indexOf(" WHERE ");
            
            if (whereIndex > 0)
            {
                // 已有WHERE子句，追加条件
                int whereEndIndex = whereIndex + 7; // " WHERE "的长度
                // 查找WHERE子句的结束位置（下一个关键字或SQL结束）
                int nextKeywordIndex = findNextKeywordIndex(upperSql, whereEndIndex);
                
                if (nextKeywordIndex > 0)
                {
                    // 在WHERE子句和下一个关键字之间插入
                    String beforeWhere = sql.substring(0, whereEndIndex);
                    String afterWhere = sql.substring(whereEndIndex, nextKeywordIndex);
                    String afterKeyword = sql.substring(nextKeywordIndex);
                    
                    // 如果WHERE子句后已有条件，添加AND
                    if (!afterWhere.trim().isEmpty())
                    {
                        return beforeWhere + afterWhere.trim() + " AND (" + combinedFilter + ") " + afterKeyword;
                    }
                    else
                    {
                        return beforeWhere + "(" + combinedFilter + ") " + afterKeyword;
                    }
                }
                else
                {
                    // WHERE子句在SQL末尾
                    return sql + " AND (" + combinedFilter + ")";
                }
            }
            else
            {
                // 没有WHERE子句，添加WHERE
                // 查找ORDER BY, GROUP BY, LIMIT等关键字的位置
                int orderByIndex = upperSql.indexOf(" ORDER BY ");
                int groupByIndex = upperSql.indexOf(" GROUP BY ");
                int havingIndex = upperSql.indexOf(" HAVING ");
                int limitIndex = upperSql.indexOf(" LIMIT ");
                
                int insertIndex = sql.length();
                if (orderByIndex > 0) insertIndex = Math.min(insertIndex, orderByIndex);
                if (groupByIndex > 0) insertIndex = Math.min(insertIndex, groupByIndex);
                if (havingIndex > 0) insertIndex = Math.min(insertIndex, havingIndex);
                if (limitIndex > 0) insertIndex = Math.min(insertIndex, limitIndex);
                
                if (insertIndex < sql.length())
                {
                    return sql.substring(0, insertIndex) + " WHERE (" + combinedFilter + ") " + sql.substring(insertIndex);
                }
                else
                {
                    return sql + " WHERE (" + combinedFilter + ")";
                }
            }
        }
        catch (Exception e)
        {
            log.error("注入行级过滤条件失败: sql={}, filters={}", sql, rowFilters, e);
            return sql;
        }
    }
    
    /**
     * 查找下一个SQL关键字的位置
     */
    private int findNextKeywordIndex(String upperSql, int startIndex)
    {
        String[] keywords = {" ORDER BY ", " GROUP BY ", " HAVING ", " LIMIT ", " UNION ", " INTERSECT ", " EXCEPT "};
        int minIndex = -1;
        
        for (String keyword : keywords)
        {
            int index = upperSql.indexOf(keyword, startIndex);
            if (index > 0 && (minIndex < 0 || index < minIndex))
            {
                minIndex = index;
            }
        }
        
        return minIndex > 0 ? minIndex : -1;
    }
}
