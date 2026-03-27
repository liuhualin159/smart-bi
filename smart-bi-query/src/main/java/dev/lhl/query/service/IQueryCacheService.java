package dev.lhl.query.service;

import java.util.List;
import java.util.Map;

/**
 * 查询结果缓存服务接口
 * 负责缓存查询结果，提高查询性能
 * 
 * @author smart-bi
 */
public interface IQueryCacheService
{
    /**
     * 获取缓存结果
     * 
     * @param cacheKey 缓存键（SQL的MD5哈希）
     * @return 缓存的结果，如果不存在返回null
     */
    CachedResult getCachedResult(String cacheKey);
    
    /**
     * 缓存查询结果
     * 
     * @param cacheKey 缓存键
     * @param data 查询结果数据
     * @param ttlSeconds 缓存过期时间（秒）
     */
    void cacheResult(String cacheKey, List<Map<String, Object>> data, int ttlSeconds);
    
    /**
     * 清除缓存
     * 
     * @param cacheKey 缓存键（可选，如果为null则清除所有缓存）
     */
    void clearCache(String cacheKey);

    /**
     * 获取命中率统计
     *
     * @return hitCount, missCount, hitRate, cacheSize
     */
    HitRateStats getHitRateStats();

    /**
     * 命中率统计
     */
    class HitRateStats {
        private final long hitCount;
        private final long missCount;
        private final int cacheSize;

        public HitRateStats(long hitCount, long missCount, int cacheSize) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.cacheSize = cacheSize;
        }
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public int getCacheSize() { return cacheSize; }
        public double getHitRate() {
            long total = hitCount + missCount;
            return total == 0 ? 0 : (double) hitCount / total;
        }
    }
    
    /**
     * 缓存结果
     */
    class CachedResult
    {
        private List<Map<String, Object>> data;
        private long expireTime;
        
        public CachedResult(List<Map<String, Object>> data, long expireTime)
        {
            this.data = data;
            this.expireTime = expireTime;
        }
        
        public boolean isExpired()
        {
            return System.currentTimeMillis() > expireTime;
        }
        
        public List<Map<String, Object>> getData() { return data; }
        public long getExpireTime() { return expireTime; }
    }
}
