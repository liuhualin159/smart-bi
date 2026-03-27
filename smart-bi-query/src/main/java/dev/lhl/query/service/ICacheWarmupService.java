package dev.lhl.query.service;

/**
 * 缓存预热服务
 *
 * @author smart-bi
 */
public interface ICacheWarmupService {

    /**
     * 预热常用查询到缓存（基于最近成功执行的查询记录）
     *
     * @param limit 最多预热的查询条数
     * @return 成功预热的数量
     */
    int warmupRecentQueries(int limit);
}
