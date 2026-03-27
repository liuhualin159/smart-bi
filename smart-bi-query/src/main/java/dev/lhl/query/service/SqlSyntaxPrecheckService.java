package dev.lhl.query.service;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * SQL 语法预检服务
 * 使用 JSqlParser 在 SQL 提交执行前做语法校验
 */
@Service
public class SqlSyntaxPrecheckService
{
    private static final Logger log = LoggerFactory.getLogger(SqlSyntaxPrecheckService.class);

    @Value("${smart.bi.nl2sql.syntaxPrecheck.enabled:true}")
    private boolean enabled;

    @Value("${smart.bi.nl2sql.syntaxPrecheck.fallbackOnParseError:false}")
    private boolean fallbackOnParseError;

    /**
     * 预检结果
     */
    public record PrecheckResult(boolean passed, String errorMessage) {}

    /**
     * 对 SQL 做语法预检（带可选方言，当前实现使用通用解析；方言预留供后续按数据源切换解析器）
     *
     * @param sql SQL 语句
     * @param dialect 方言标识（如 mysql、postgresql），可为 null，当前未使用
     * @return 预检结果；passed=true 表示通过或已降级放行
     */
    public PrecheckResult check(String sql, String dialect)
    {
        return check(sql);
    }

    /**
     * 对 SQL 做语法预检
     *
     * @param sql SQL 语句
     * @return 预检结果；passed=true 表示通过或已降级放行
     */
    public PrecheckResult check(String sql)
    {
        if (!enabled)
        {
            return new PrecheckResult(true, null);
        }

        if (sql == null || sql.trim().isEmpty())
        {
            return new PrecheckResult(false, "SQL 语法预检失败：SQL 为空");
        }

        try
        {
            String trimmed = sql.trim();
            if (trimmed.endsWith(";"))
            {
                trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
            }
            CCJSqlParserUtil.parse(trimmed);
            log.debug("SQL 语法预检通过");
            return new PrecheckResult(true, null);
        }
        catch (Exception e)
        {
            String errorDetail = e.getMessage();
            if (errorDetail != null && errorDetail.length() > 300)
            {
                errorDetail = errorDetail.substring(0, 300) + "...";
            }
            String errorMsg = "SQL 语法预检失败：" + errorDetail;

            if (fallbackOnParseError)
            {
                log.warn("SQL 语法预检失败（fallback=true，继续执行）: {}", errorDetail);
                return new PrecheckResult(true, null);
            }
            else
            {
                log.warn("SQL 语法预检失败: {}", errorDetail);
                return new PrecheckResult(false, errorMsg);
            }
        }
    }
}
