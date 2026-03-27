package dev.lhl.query.service;

import java.util.List;
import java.util.Map;

/**
 * 供 NL2SQL/LLM 使用的元数据工具服务
 * 提供「全部可用表名与注释」「指定表结构」等，用于禁止表名幻觉并辅助 SQL 生成
 *
 * @author smart-bi
 */
public interface ITableMetadataToolService
{
    /**
     * 列出全部可用表名及注释（工具：list_tables_with_comments）
     * 用于在提示词中约束 LLM 仅使用这些表名
     *
     * @return 表名与注释的列表，每项含 table_name、comment
     */
    List<Map<String, String>> listTablesWithComments();

    /**
     * 格式化为提示词中使用的「全部可用表」文本（一行一表：表名（注释））
     *
     * @return 多行文本，无表时返回空字符串
     */
    String formatAllTablesListForPrompt();

    /**
     * 获取指定表的表结构（工具：get_table_schema）
     *
     * @param tableName 表名（与 bi_table_metadata.table_name 匹配，忽略大小写）
     * @return 表名 + 字段列表（name、type、comment），不存在则返回 null
     */
    Map<String, Object> getTableSchema(String tableName);
}
