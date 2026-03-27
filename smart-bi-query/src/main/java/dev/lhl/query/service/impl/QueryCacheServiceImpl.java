package dev.lhl.query.service.impl;

import dev.lhl.query.service.IQueryCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 查询结果缓存服务实现
 * 使用内存缓存（可替换为Redis），支持缓存预热、命中率分析
 *
 * @author smart-bi
 */
@Service
public class QueryCacheServiceImpl implements IQueryCacheService
{
    private static final Logger log = LoggerFactory.getLogger(QueryCacheServiceImpl.class);

    // 内存缓存（生产环境建议使用Redis）
    private final ConcurrentHashMap<String, CachedResult> cache = new ConcurrentHashMap<>();

    // 命中率统计
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);

    // 最大缓存条目数
    private static final int MAX_CACHE_SIZE = 1000;
    
    @Override
    public CachedResult getCachedResult(String cacheKey)
    {
        if (cacheKey == null)
        {
            return null;
        }
        
        CachedResult cached = cache.get(cacheKey);
        if (cached == null)
        {
            missCount.incrementAndGet();
            return null;
        }

        // 检查是否过期
        if (cached.isExpired())
        {
            cache.remove(cacheKey);
            missCount.incrementAndGet();
            log.debug("缓存已过期: cacheKey={}", cacheKey);
            return null;
        }

        hitCount.incrementAndGet();
        log.debug("从缓存获取结果: cacheKey={}", cacheKey);
        return cached;
    }
    
    @Override
    public void cacheResult(String cacheKey, List<Map<String, Object>> data, int ttlSeconds)
    {
        if (cacheKey == null || data == null)
        {
            return;
        }
        
        // 如果缓存已满，清除最旧的条目（简单策略：清除第一个）
        if (cache.size() >= MAX_CACHE_SIZE)
        {
            String firstKey = cache.keySet().iterator().next();
            cache.remove(firstKey);
            log.debug("缓存已满，清除最旧条目: cacheKey={}", firstKey);
        }
        
        long expireTime = System.currentTimeMillis() + (ttlSeconds * 1000L);
        CachedResult cached = new CachedResult(data, expireTime);
        cache.put(cacheKey, cached);
        
        log.debug("缓存查询结果: cacheKey={}, ttl={}s", cacheKey, ttlSeconds);
    }
    
    @Override
    public void clearCache(String cacheKey)
    {
        if (cacheKey == null)
        {
            // 清除所有缓存
            cache.clear();
            log.info("清除所有查询缓存");
        }
        else
        {
            cache.remove(cacheKey);
            log.debug("清除缓存: cacheKey={}", cacheKey);
        }
    }

    @Override
    public HitRateStats getHitRateStats()
    {
        return new HitRateStats(hitCount.get(), missCount.get(), cache.size());
    }
}
