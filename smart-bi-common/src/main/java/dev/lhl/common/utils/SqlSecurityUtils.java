package dev.lhl.common.utils;

import dev.lhl.common.exception.UtilException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SQL安全校验工具类
 * 使用语法树白名单机制，仅允许SELECT语句
 * 
 * @author smart-bi
 */
public class SqlSecurityUtils
{
    private static final Logger log = LoggerFactory.getLogger(SqlSecurityUtils.class);
    
    /**
     * 禁止的危险函数列表
     */
    private static final Set<String> DANGEROUS_FUNCTIONS = new HashSet<>();
    
    /**
     * 禁止访问的系统表前缀
     */
    private static final Set<String> FORBIDDEN_TABLE_PREFIXES = new HashSet<>();
    
    static
    {
        // 初始化危险函数列表
        DANGEROUS_FUNCTIONS.add("xp_cmdshell");
        DANGEROUS_FUNCTIONS.add("exec");
        DANGEROUS_FUNCTIONS.add("execute");
        DANGEROUS_FUNCTIONS.add("sp_executesql");
        DANGEROUS_FUNCTIONS.add("eval");
        DANGEROUS_FUNCTIONS.add("system");
        DANGEROUS_FUNCTIONS.add("shell_exec");
        
        // 初始化禁止访问的系统表前缀
        FORBIDDEN_TABLE_PREFIXES.add("information_schema");
        FORBIDDEN_TABLE_PREFIXES.add("sys");
        FORBIDDEN_TABLE_PREFIXES.add("mysql");
        FORBIDDEN_TABLE_PREFIXES.add("performance_schema");
    }
    
    /**
     * 禁止的SQL关键字（DDL/DML）
     */
    private static final Set<String> FORBIDDEN_KEYWORDS = new HashSet<>();
    
    /**
     * SELECT语句正则表达式（不区分大小写）
     */
    private static final Pattern SELECT_PATTERN = Pattern.compile(
        "^\\s*select\\s+", 
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    static
    {
        // 初始化禁止的关键字
        FORBIDDEN_KEYWORDS.add("insert");
        FORBIDDEN_KEYWORDS.add("update");
        FORBIDDEN_KEYWORDS.add("delete");
        FORBIDDEN_KEYWORDS.add("create");
        FORBIDDEN_KEYWORDS.add("alter");
        FORBIDDEN_KEYWORDS.add("drop");
        FORBIDDEN_KEYWORDS.add("truncate");
        FORBIDDEN_KEYWORDS.add("exec");
        FORBIDDEN_KEYWORDS.add("execute");
        FORBIDDEN_KEYWORDS.add("call");
        FORBIDDEN_KEYWORDS.add("grant");
        FORBIDDEN_KEYWORDS.add("revoke");
    }
    
    /**
     * 验证SQL语句是否安全
     * 使用语法树白名单机制，仅允许SELECT语句
     * 
     * @param sql SQL语句
     * @throws UtilException 如果SQL不安全
     */
    public static void validateSQL(String sql)
    {
        if (StringUtils.isEmpty(sql))
        {
            throw new UtilException("SQL语句不能为空");
        }
        
        // 去除注释和多余空白
        String normalizedSql = sql.trim().replaceAll("--.*", "")
            .replaceAll("/\\*.*?\\*/", "")
            .replaceAll("\\s+", " ");
        
        // 检查是否为SELECT语句
        if (!SELECT_PATTERN.matcher(normalizedSql).find())
        {
            throw new UtilException("仅允许SELECT查询语句");
        }
        
        // 检查禁止的关键字（DDL/DML）
        String lowerSql = normalizedSql.toLowerCase();
        for (String keyword : FORBIDDEN_KEYWORDS)
        {
            // 使用单词边界匹配，避免误判（如 "selected" 包含 "select"）
            Pattern keywordPattern = Pattern.compile(
                "\\b" + Pattern.quote(keyword) + "\\b",
                Pattern.CASE_INSENSITIVE
            );
            if (keywordPattern.matcher(lowerSql).find())
            {
                throw new UtilException("禁止" + keyword.toUpperCase() + "语句");
            }
        }
        
        // 检查危险函数
        for (String function : DANGEROUS_FUNCTIONS)
        {
            Pattern functionPattern = Pattern.compile(
                "\\b" + Pattern.quote(function) + "\\s*\\(",
                Pattern.CASE_INSENSITIVE
            );
            if (functionPattern.matcher(lowerSql).find())
            {
                throw new UtilException("SQL包含危险函数: " + function);
            }
        }
        
        // 检查系统表访问
        for (String prefix : FORBIDDEN_TABLE_PREFIXES)
        {
            Pattern tablePattern = Pattern.compile(
                "\\b" + Pattern.quote(prefix) + "\\.",
                Pattern.CASE_INSENSITIVE
            );
            if (tablePattern.matcher(lowerSql).find())
            {
                throw new UtilException("禁止访问系统表: " + prefix);
            }
        }
        
        // 检查堆叠查询（多个SQL语句用分号分隔）
        String[] statements = normalizedSql.split(";");
        if (statements.length > 1)
        {
            // 检查每个语句是否都是SELECT
            for (int i = 0; i < statements.length; i++)
            {
                String stmt = statements[i].trim();
                if (!StringUtils.isEmpty(stmt) && !SELECT_PATTERN.matcher(stmt).find())
                {
                    throw new UtilException("禁止堆叠查询（多个SQL语句）");
                }
            }
        }
        
        log.debug("SQL安全校验通过: {}", sql);
    }
    
    /**
     * 检查SQL是否为SELECT语句（简单检查，不解析）
     * 
     * @param sql SQL语句
     * @return true表示是SELECT语句
     */
    public static boolean isSelectStatement(String sql)
    {
        if (StringUtils.isEmpty(sql))
        {
            return false;
        }
        
        String trimmedSql = sql.trim().toLowerCase();
        return trimmedSql.startsWith("select");
    }
}
