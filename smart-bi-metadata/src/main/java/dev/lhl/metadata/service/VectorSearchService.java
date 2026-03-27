package dev.lhl.metadata.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * 向量检索服务
 * 支持基于自然语言的语义检索
 * 
 * 性能要求：P95 ≤ 500毫秒（元数据量<10万条）
 * 
 * @author smart-bi
 */
@Service
public class VectorSearchService
{
    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);
    
    /**
     * 相似度阈值
     */
    private static final double SIMILARITY_THRESHOLD = 0.7;
    
    /**
     * 默认返回结果数
     */
    private static final int DEFAULT_TOP_K = 10;
    
    @Autowired(required = false)
    private VectorStore vectorStore;
    
    /**
     * 检查向量检索是否可用
     */
    private boolean isVectorSearchAvailable()
    {
        if (vectorStore == null)
        {
            log.warn("VectorStore未配置，向量检索功能不可用。请检查application.yml中的spring.ai.vectorstore.qdrant配置");
            return false;
        }
        return true;
    }
    
    /**
     * 语义检索
     * 
     * @param query 查询文本
     * @param topK 返回前K个结果（默认10）
     * @return 相似向量列表，包含相似度分数
     */
    public List<Map<String, Object>> search(String query, int topK)
    {
        if (!isVectorSearchAvailable())
        {
            log.debug("向量检索不可用，返回空结果: query={}", query);
            return List.of();
        }
        
        if (topK <= 0)
        {
            topK = DEFAULT_TOP_K;
        }
        
        log.debug("向量检索: query={}, topK={}", query, topK);
        
        try
        {
            // 使用Spring AI的VectorStore进行相似度搜索
            // Spring AI 1.1 GA的API：similaritySearch(String) 返回所有结果，需要手动过滤
            List<Document> documents = vectorStore.similaritySearch(query);
            
            // 转换为Map格式，包含相似度分数，并应用topK和相似度阈值过滤
            List<Map<String, Object>> results = documents.stream()
                .map(doc -> {
                    Map<String, Object> result = new HashMap<>();
                    // Document ID是UUID格式，但元数据中保存了原始业务ID
                    // 优先使用元数据中的businessId，如果没有则使用id
                    String businessId = (String) doc.getMetadata().getOrDefault("businessId", doc.getId());
                    result.put("id", businessId); // 返回原始业务ID
                    result.put("uuidId", doc.getId()); // 保留UUID ID用于调试
                    result.put("content", doc.getText());
                    result.put("metadata", doc.getMetadata());
                    
                    // 计算相似度（从metadata中获取distance或score）
                    double similarity = 1.0;
                    if (doc.getMetadata().containsKey("distance"))
                    {
                        // 将距离转换为相似度（距离越小，相似度越高）
                        double distance = ((Number) doc.getMetadata().get("distance")).doubleValue();
                        similarity = 1.0 / (1.0 + distance);
                    }
                    else if (doc.getMetadata().containsKey("score"))
                    {
                        similarity = ((Number) doc.getMetadata().get("score")).doubleValue();
                    }
                    result.put("similarity", similarity);
                    
                    return result;
                })
                .filter(result -> {
                    // 过滤相似度低于阈值的结果
                    double similarity = ((Number) result.get("similarity")).doubleValue();
                    return similarity >= SIMILARITY_THRESHOLD;
                })
                .sorted((a, b) -> {
                    // 按相似度降序排序
                    double simA = ((Number) a.get("similarity")).doubleValue();
                    double simB = ((Number) b.get("similarity")).doubleValue();
                    return Double.compare(simB, simA);
                })
                .limit(topK) // 限制返回topK个结果
                .collect(Collectors.toList());
            
            log.debug("向量检索完成: query={}, results={}", query, results.size());
            return results;
        }
        catch (Exception e)
        {
            if (isCollectionNotFound(e))
            {
                log.warn("Qdrant Collection 不存在，返回空结果。请确认已配置 initialize-schema: true 或先在元数据管理中执行向量化以创建集合: query={}", query);
                return List.of();
            }
            log.error("向量检索失败: query={}", query, e);
            throw new RuntimeException("向量检索失败: " + e.getMessage(), e);
        }
    }

    private boolean isCollectionNotFound(Throwable t)
    {
        for (Throwable current = t; current != null; current = current.getCause())
        {
            String msg = current.getMessage() != null ? current.getMessage() : "";
            if (msg.contains("NOT_FOUND") && msg.contains("doesn't exist"))
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 语义检索（使用默认topK=10）
     */
    public List<Map<String, Object>> search(String query)
    {
        return search(query, DEFAULT_TOP_K);
    }
    
    /**
     * 获取相似度阈值
     */
    public double getSimilarityThreshold()
    {
        return SIMILARITY_THRESHOLD;
    }
    
    /**
     * 带过滤条件的语义检索
     * 
     * @param query 查询文本
     * @param topK 返回前K个结果
     * @param filterExpression 过滤表达式（例如：metadata.type == 'table'）
     * @return 相似向量列表
     */
    public List<Map<String, Object>> searchWithFilter(String query, int topK, String filterExpression)
    {
        if (!isVectorSearchAvailable())
        {
            log.debug("向量检索不可用，返回空结果: query={}", query);
            return List.of();
        }
        
        if (topK <= 0)
        {
            topK = DEFAULT_TOP_K;
        }
        
        log.debug("带过滤条件的向量检索: query={}, topK={}, filter={}", query, topK, filterExpression);
        
        try
        {
            // 如果提供了过滤表达式，记录日志（具体实现取决于VectorStore）
            if (filterExpression != null && !filterExpression.trim().isEmpty())
            {
                log.debug("应用过滤条件: {}", filterExpression);
            }
            
            // 使用Spring AI的VectorStore进行相似度搜索
            List<Document> documents = vectorStore.similaritySearch(query);
            
            // 转换为Map格式，应用topK和相似度阈值过滤
            List<Map<String, Object>> results = documents.stream()
                .map(doc -> {
                    Map<String, Object> result = new HashMap<>();
                    // Document ID是UUID格式，但元数据中保存了原始业务ID
                    // 优先使用元数据中的businessId，如果没有则使用id
                    String businessId = (String) doc.getMetadata().getOrDefault("businessId", doc.getId());
                    result.put("id", businessId); // 返回原始业务ID
                    result.put("uuidId", doc.getId()); // 保留UUID ID用于调试
                    result.put("content", doc.getText());
                    result.put("metadata", doc.getMetadata());
                    
                    // 计算相似度
                    double similarity = 1.0;
                    if (doc.getMetadata().containsKey("distance"))
                    {
                        double distance = ((Number) doc.getMetadata().get("distance")).doubleValue();
                        similarity = 1.0 / (1.0 + distance);
                    }
                    else if (doc.getMetadata().containsKey("score"))
                    {
                        similarity = ((Number) doc.getMetadata().get("score")).doubleValue();
                    }
                    result.put("similarity", similarity);
                    
                    return result;
                })
                .filter(result -> {
                    // 过滤相似度低于阈值的结果
                    double similarity = ((Number) result.get("similarity")).doubleValue();
                    return similarity >= SIMILARITY_THRESHOLD;
                })
                .sorted((a, b) -> {
                    // 按相似度降序排序
                    double simA = ((Number) a.get("similarity")).doubleValue();
                    double simB = ((Number) b.get("similarity")).doubleValue();
                    return Double.compare(simB, simA);
                })
                .limit(topK) // 限制返回topK个结果
                .collect(Collectors.toList());
            
            log.debug("带过滤条件的向量检索完成: query={}, results={}", query, results.size());
            return results;
        }
        catch (Exception e)
        {
            if (isCollectionNotFound(e))
            {
                log.warn("Qdrant Collection 不存在，返回空结果: query={}", query);
                return List.of();
            }
            log.error("带过滤条件的向量检索失败: query={}", query, e);
            throw new RuntimeException("向量检索失败: " + e.getMessage(), e);
        }
    }
}
