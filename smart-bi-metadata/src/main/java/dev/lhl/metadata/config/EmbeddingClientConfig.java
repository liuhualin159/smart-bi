package dev.lhl.metadata.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding客户端配置
 * 
 * 注意：Spring AI 1.1.0 可能不支持独立的 embedding base-url 配置
 * 根据阿里云官方示例，对话和向量模型使用相同的base-url
 * 因此不配置 embedding.base-url，让 Spring AI 自动使用 openai.base-url
 * 
 * 配置方式：
 * spring:
 *   ai:
 *     openai:
 *       api-key: ${DASHSCOPE_API_KEY:sk-your-api-key}
 *       base-url: https://dashscope.aliyuncs.com/compatible-mode/v1  # 对话和向量共用
 *       embedding:
 *         # 不配置 base-url，使用 openai.base-url
 *         # 不配置 api-key，使用 openai.api-key
 *         options:
 *           model: text-embedding-v4
 *           dimensions: 1024
 * 
 * @author smart-bi
 */
@Configuration
public class EmbeddingClientConfig
{
    private static final Logger log = LoggerFactory.getLogger(EmbeddingClientConfig.class);
    
    @Autowired(required = false)
    private EmbeddingConfig embeddingConfig;
    
    /**
     * 初始化配置
     * 记录配置信息
     */
    public EmbeddingClientConfig()
    {
        log.info("EmbeddingClientConfig初始化");
        log.info("Spring AI将使用 spring.ai.openai.base-url 作为embedding的base-url");
        
        if (embeddingConfig != null)
        {
            if (embeddingConfig.getModel() != null)
            {
                log.info("Embedding model: {}", embeddingConfig.getModel());
            }
            
            if (embeddingConfig.getDimensions() > 0)
            {
                log.info("Embedding dimensions: {}", embeddingConfig.getDimensions());
            }
        }
        else
        {
            log.debug("EmbeddingConfig未注入，使用Spring AI默认配置");
        }
    }
}
