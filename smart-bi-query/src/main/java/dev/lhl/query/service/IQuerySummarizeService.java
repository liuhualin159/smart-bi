package dev.lhl.query.service;

import java.util.List;
import java.util.Map;

/**
 * 查询结果总结服务：调用 LLM 输出 1~3 句总结
 *
 * @author smart-bi
 */
public interface IQuerySummarizeService {

    /**
     * 根据查询结果生成 1~3 句总结
     *
     * @param queryId   查询记录 ID（可选，用于上下文）
     * @param chartType 图表类型（可选）
     * @param columns   列名列表
     * @param data      结果数据（前若干行）
     * @return 总结文本；LLM 不可用或未启用时返回 null
     */
    String summarize(Long queryId, String chartType, List<String> columns, List<Map<String, Object>> data);
}
