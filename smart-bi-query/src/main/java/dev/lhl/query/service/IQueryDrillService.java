package dev.lhl.query.service;

import dev.lhl.query.domain.QueryRecord;

import java.util.Map;

/**
 * 下钻服务：在原始 SQL 上追加 AND {dimension}={value}，应用权限注入后执行
 *
 * @author smart-bi
 */
public interface IQueryDrillService {

    /**
     * 下钻执行：基于原查询记录，追加维度条件后执行
     *
     * @param queryId        原查询记录 ID
     * @param drillDimension 下钻维度字段名
     * @param drillValue     下钻维度值
     * @param userId         用户 ID
     * @return 执行结果；若原记录不存在或 SQL 追加失败则抛出异常
     */
    DrillResult drill(Long queryId, String drillDimension, Object drillValue, Long userId);

    record DrillResult(QueryRecord record, java.util.List<Map<String, Object>> data, long rowCount, long executionTime) {}
}
