package dev.lhl.query.service;

import dev.lhl.query.domain.QueryRecord;

/**
 * NL2SQL服务接口
 * 
 * @author smart-bi
 */
public interface INl2SqlService
{
    /**
     * 将自然语言问题转换为SQL
     * 
     * @param question 自然语言问题
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 查询记录（包含生成的SQL）
     */
    QueryRecord generateSQL(String question, Long userId, Long sessionId);

    /**
     * 执行失败时根据数据库错误信息自修正 SQL，并完成安全校验与权限注入，返回可再次执行的查询记录。
     * SqlSecurityUtils 校验或权限注入失败时不重试，返回 null。
     *
     * @param question 用户原始问题
     * @param currentRecord 当前查询记录（含失败的 SQL）
     * @param dbErrorMessage 数据库返回的错误信息
     * @param userId 用户ID
     * @return 更新后的 QueryRecord（含 generatedSql、executedSql），可交给执行服务再次执行；修正失败或安全/权限不通过时返回 null
     */
    QueryRecord correctAndPrepareForRetry(String question, QueryRecord currentRecord, String dbErrorMessage, Long userId);
}
