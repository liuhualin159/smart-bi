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
import java.util.UUID;

/**
 * 向量存储服务
 * 使用Spring AI的向量存储接口集成Qdrant
 * 
 * @author smart-bi
 */
@Service
public class VectorStoreService
{
    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);
    
    @Autowired(required = false)
    private VectorStore vectorStore;
    
    /**
     * 检查VectorStore是否可用
     */
    private boolean isVectorStoreAvailable()
    {
        if (vectorStore == null)
        {
            log.warn("VectorStore未配置，向量存储功能不可用。请检查application.yml中的spring.ai.vectorstore.qdrant配置");
            return false;
        }
        return true;
    }
    
    /**
     * 将业务ID转换为UUID格式
     * Qdrant VectorStore要求Document ID必须是UUID格式
     * 使用UUID.nameUUIDFromBytes()基于业务ID生成稳定的UUID
     * 
     * @param businessId 业务ID（如：business_domain_12）
     * @return UUID格式的字符串
     */
    private String toUUID(String businessId)
    {
        if (businessId == null || businessId.isEmpty())
        {
            // 如果业务ID为空，生成新的UUID
            return UUID.randomUUID().toString();
        }
        
        // 检查是否已经是UUID格式
        try
        {
            UUID.fromString(businessId);
            return businessId; // 已经是UUID格式，直接返回
        }
        catch (IllegalArgumentException e)
        {
            // 不是UUID格式，基于业务ID生成稳定的UUID
            // 使用UUID.nameUUIDFromBytes()确保相同业务ID总是生成相同的UUID
            UUID uuid = UUID.nameUUIDFromBytes(businessId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return uuid.toString();
        }
    }
    
    /**
     * 存储向量
     * 
     * @param id 向量ID
     * @param text 文本内容
     * @param metadata 元数据
     */
    public void store(String id, String text, Map<String, Object> metadata)
    {
        if (!isVectorStoreAvailable())
        {
            log.debug("VectorStore不可用，跳过向量存储: id={}", id);
            return;
        }
        
        try
        {
            log.info("开始存储向量: id={}, text长度={}", id, text != null ? text.length() : 0);
            
            // Qdrant VectorStore要求Document ID必须是UUID格式
            // 将业务ID转换为UUID格式
            String uuidId = toUUID(id);
            log.debug("业务ID转换为UUID: {} -> {}", id, uuidId);
            
            // 创建Document对象，包含文本和元数据
            Map<String, Object> documentMetadata = new HashMap<>();
            if (metadata != null)
            {
                documentMetadata.putAll(metadata);
            }
            // 添加原始业务ID到元数据中，用于后续查询和删除
            documentMetadata.put("businessId", id);
            documentMetadata.put("id", id); // 保留原始ID用于查询
            
            // 使用UUID作为Document ID
            Document document = new Document(uuidId, text, documentMetadata);
            
            // 使用Spring AI的VectorStore存储
            // 注意：QdrantVectorStore.add() 会先调用 embeddingModel.dimensions() 获取向量维度
            // 如果向量化API调用失败，会在这里抛出异常
            log.debug("调用VectorStore.add()，将触发向量化API调用");
            vectorStore.add(List.of(document));
            
            log.info("向量存储成功: businessId={}, uuidId={}", id, uuidId);
        }
        catch (Exception e)
        {
            // 分析错误类型
            String errorType = "未知错误";
            String errorDetail = e.getMessage();
            
            if (e.getMessage() != null)
            {
                if (e.getMessage().contains("HTTP 404"))
                {
                    errorType = "向量化API调用失败（HTTP 404）";
                    errorDetail = "请检查Spring AI的embedding API配置，确认base-url是否正确。实际请求的URL可能不正确。";
                }
                else if (e.getMessage().contains("HTTP 401") || e.getMessage().contains("HTTP 403"))
                {
                    errorType = "向量化API认证失败";
                    errorDetail = "请检查API Key配置是否正确。";
                }
                else if (e.getMessage().contains("Connection") || e.getMessage().contains("timeout"))
                {
                    errorType = "向量化API网络连接失败";
                    errorDetail = "请检查网络连接和API端点是否可访问。";
                }
            }
            
            log.error("向量存储失败: id={}, 错误类型={}, 错误详情={}", id, errorType, errorDetail, e);
            throw new RuntimeException("向量存储失败: " + errorType + " - " + errorDetail, e);
        }
    }
    
    /**
     * 删除向量
     * 
     * @param id 向量ID
     */
    public void delete(String id)
    {
        if (!isVectorStoreAvailable())
        {
            log.debug("VectorStore不可用，跳过向量删除: id={}", id);
            return;
        }
        
        try
        {
            log.debug("删除向量: id={}", id);
            
            // Qdrant VectorStore要求Document ID必须是UUID格式
            // 将业务ID转换为UUID格式
            String uuidId = toUUID(id);
            log.debug("业务ID转换为UUID: {} -> {}", id, uuidId);
            
            // 使用Spring AI的VectorStore删除
            // 注意：Spring AI的VectorStore.delete方法需要传入Document ID列表（UUID格式）
            vectorStore.delete(List.of(uuidId));
            
            log.debug("向量删除成功: businessId={}, uuidId={}", id, uuidId);
        }
        catch (Exception e)
        {
            log.error("向量删除失败: id={}", id, e);
            throw new RuntimeException("向量删除失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新向量
     * 
     * @param id 向量ID
     * @param text 新文本内容
     * @param metadata 新元数据
     */
    public void update(String id, String text, Map<String, Object> metadata)
    {
        if (!isVectorStoreAvailable())
        {
            log.debug("VectorStore不可用，跳过向量更新: id={}", id);
            return;
        }
        
        try
        {
            log.debug("更新向量: id={}", id);
            
            // 先删除旧向量
            delete(id);
            
            // 再存储新向量
            store(id, text, metadata);
            
            log.debug("向量更新成功: id={}", id);
        }
        catch (Exception e)
        {
            log.error("向量更新失败: id={}", id, e);
            throw new RuntimeException("向量更新失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量存储向量
     * 
     * @param documents 文档列表，每个文档包含id、text和metadata
     */
    public void storeBatch(List<Map<String, Object>> documents)
    {
        if (!isVectorStoreAvailable())
        {
            log.debug("VectorStore不可用，跳过批量向量存储");
            return;
        }
        
        try
        {
            log.debug("批量存储向量: count={}", documents.size());
            
            List<Document> documentList = documents.stream()
                .map(doc -> {
                    String businessId = (String) doc.get("id");
                    String text = (String) doc.get("text");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metadata = (Map<String, Object>) doc.getOrDefault("metadata", new HashMap<>());
                    
                    // Qdrant VectorStore要求Document ID必须是UUID格式
                    String uuidId = toUUID(businessId);
                    
                    Map<String, Object> documentMetadata = new HashMap<>(metadata);
                    documentMetadata.put("businessId", businessId); // 保留原始业务ID
                    documentMetadata.put("id", businessId); // 保留原始ID用于查询
                    
                    // 使用UUID作为Document ID
                    return new Document(uuidId, text, documentMetadata);
                })
                .toList();
            
            vectorStore.add(documentList);
            
            log.debug("批量向量存储成功: count={}", documents.size());
        }
        catch (Exception e)
        {
            log.error("批量向量存储失败", e);
            throw new RuntimeException("批量向量存储失败: " + e.getMessage(), e);
        }
    }
}
