package dev.lhl.query.service;

import java.util.List;
import java.util.Map;

/**
 * 慢查询监控服务
 *
 * @author smart-bi
 */
public interface ISlowQueryService {

    /**
     * 获取慢查询列表（执行时间超过阈值的记录）
     *
     * @param thresholdMs 阈值毫秒数（默认 5000）
     * @param limit 最大条数
     * @return 慢查询记录列表
     */
    List<Map<String, Object>> getSlowQueries(long thresholdMs, int limit);
}
