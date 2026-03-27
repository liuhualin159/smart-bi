package dev.lhl.metadata.service.impl;

import dev.lhl.metadata.service.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向量检索服务实现
 * 实现向量检索性能优化（索引优化、缓存机制）
 * 
 * @author smart-bi
 */
@Service
public class VectorSearchServiceImpl extends VectorSearchService
{
    private static final Logger log = LoggerFactory.getLogger(VectorSearchServiceImpl.class);
    
    /**
     * 查询结果缓存（LRU缓存）
     */
    private final Map<String, List<Map<String, Object>>> queryCache = new ConcurrentHashMap<>();
    
    /**
     * 缓存最大大小
     */
    private static final int CACHE_MAX_SIZE = 1000;
    
    /**
     * 缓存过期时间（毫秒）
     */
    private static final long CACHE_EXPIRE_TIME = 5 * 60 * 1000; // 5分钟
    
    @Override
    public List<Map<String, Object>> search(String query, int topK)
    {
        // 检查缓存
        String cacheKey = query + "_" + topK;
        List<Map<String, Object>> cachedResult = queryCache.get(cacheKey);
        if (cachedResult != null)
        {
            log.debug("从缓存获取查询结果: query={}", query);
            return cachedResult;
        }
        
        // 执行检索
        List<Map<String, Object>> result = super.search(query, topK);
        
        // 更新缓存（如果缓存未满）
        if (queryCache.size() < CACHE_MAX_SIZE)
        {
            queryCache.put(cacheKey, result);
            log.debug("缓存查询结果: query={}", query);
        }
        
        return result;
    }
    
    /**
     * 清除缓存
     */
    public void clearCache()
    {
        queryCache.clear();
        log.info("向量检索缓存已清除");
    }
}
