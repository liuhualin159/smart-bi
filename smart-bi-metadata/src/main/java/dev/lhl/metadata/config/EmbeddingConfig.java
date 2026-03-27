package dev.lhl.metadata.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding模型配置类
 * 支持单独配置向量模型的API端点（与对话模型分离）
 * 
 * 用于支持阿里云百炼等平台，其中对话模型和向量模型的base-url不同
 * 
 * @author smart-bi
 */
@Configuration
@ConfigurationProperties(prefix = "spring.ai.openai.embedding")
public class EmbeddingConfig
{
    private static final Logger log = LoggerFactory.getLogger(EmbeddingConfig.class);
    
    /**
     * API密钥（如果与对话模型不同，可以单独配置）
     */
    private String apiKey;
    
    /**
     * Base URL（向量模型的API端点）
     * 阿里云百炼向量模型：https://dashscope.aliyuncs.com/compatible-mode/v1
     * OpenAI：https://api.openai.com/v1
     */
    private String baseUrl;
    
    /**
     * 模型名称
     * 阿里云百炼：text-embedding-v1, text-embedding-v2
     * OpenAI：text-embedding-ada-002
     */
    private String model = "text-embedding-ada-002";
    
    /**
     * 向量维度
     * 根据模型确定：text-embedding-ada-002=1536, text-embedding-v1=1536
     */
    private int dimensions = 1536;
    
    /**
     * 超时时间（秒）
     */
    private int timeout = 30;
    
    public String getApiKey()
    {
        return apiKey;
    }
    
    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
        log.debug("设置Embedding API Key: {}", apiKey != null ? "已设置" : "未设置");
    }
    
    public String getBaseUrl()
    {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
        log.debug("设置Embedding Base URL: {}", baseUrl);
    }
    
    public String getModel()
    {
        return model;
    }
    
    public void setModel(String model)
    {
        this.model = model;
        log.debug("设置Embedding模型: {}", model);
    }
    
    public int getDimensions()
    {
        return dimensions;
    }
    
    public void setDimensions(int dimensions)
    {
        this.dimensions = dimensions;
        log.debug("设置向量维度: {}", dimensions);
    }
    
    public int getTimeout()
    {
        return timeout;
    }
    
    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }
}
