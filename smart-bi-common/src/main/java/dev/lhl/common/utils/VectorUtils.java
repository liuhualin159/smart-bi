package dev.lhl.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

/**
 * 向量化工具类
 * 封装Qdrant操作，提供向量存储和检索的便捷方法
 * 
 * @deprecated 此类已废弃，请使用以下服务替代：
 * - {@link dev.lhl.metadata.service.VectorStoreService} - 向量存储服务
 * - {@link dev.lhl.metadata.service.VectorSearchService} - 向量检索服务
 * 
 * 这些服务已完整实现，使用Spring AI的VectorStore接口集成Qdrant
 * 
 * @author smart-bi
 */
@Deprecated
public class VectorUtils
{
    private static final Logger log = LoggerFactory.getLogger(VectorUtils.class);
    
    /**
     * 存储向量
     * 
     * @deprecated 请使用 {@link dev.lhl.metadata.service.VectorStoreService#store(String, String, Map)} 替代
     * 
     * @param id 向量ID
     * @param text 文本内容
     * @param metadata 元数据
     */
    @Deprecated
    public static void store(String id, String text, Map<String, Object> metadata)
    {
        log.warn("VectorUtils.store() 已废弃，请使用 VectorStoreService.store() 替代: id={}", id);
        throw new UnsupportedOperationException("VectorUtils已废弃，请使用VectorStoreService替代");
    }
    
    /**
     * 检索相似向量
     * 
     * @deprecated 请使用 {@link dev.lhl.metadata.service.VectorSearchService#search(String, int)} 替代
     * 
     * @param query 查询文本
     * @param topK 返回前K个结果
     * @return 相似向量列表
     */
    @Deprecated
    public static List<Map<String, Object>> search(String query, int topK)
    {
        log.warn("VectorUtils.search() 已废弃，请使用 VectorSearchService.search() 替代: query={}", query);
        throw new UnsupportedOperationException("VectorUtils已废弃，请使用VectorSearchService替代");
    }
    
    /**
     * 删除向量
     * 
     * @deprecated 请使用 {@link dev.lhl.metadata.service.VectorStoreService#delete(List)} 替代
     * 
     * @param id 向量ID
     */
    @Deprecated
    public static void delete(String id)
    {
        log.warn("VectorUtils.delete() 已废弃，请使用 VectorStoreService.delete() 替代: id={}", id);
        throw new UnsupportedOperationException("VectorUtils已废弃，请使用VectorStoreService替代");
    }
    
    /**
     * 检查向量存储服务是否可用
     * 
     * @deprecated 请使用 {@link dev.lhl.metadata.service.VectorStoreService} 或 {@link dev.lhl.metadata.service.VectorSearchService} 替代
     * 
     * @return true表示可用
     */
    @Deprecated
    public static boolean isAvailable()
    {
        log.warn("VectorUtils.isAvailable() 已废弃，请使用 VectorStoreService 或 VectorSearchService 替代");
        return false;
    }
}
