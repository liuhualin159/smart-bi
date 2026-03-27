package dev.lhl.query.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI配置类
 * 配置大模型API连接信息
 * 
 * @author smart-bi
 */
@Configuration
@ConfigurationProperties(prefix = "spring.ai.openai")
public class SpringAIConfig
{
    private static final Logger log = LoggerFactory.getLogger(SpringAIConfig.class);
    
    /**
     * API密钥
     */
    private String apiKey;
    
    /**
     * Base URL
     */
    private String baseUrl = "https://api.openai.com";
    
    /**
     * 模型名称
     */
    private String model = "gpt-4";
    
    /**
     * 温度参数
     */
    private double temperature = 0.7;
    
    /**
     * 最大Token数
     */
    private int maxTokens = 2000;
    
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
    }
    
    public String getBaseUrl()
    {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }
    
    public String getModel()
    {
        return model;
    }
    
    public void setModel(String model)
    {
        this.model = model;
    }
    
    public double getTemperature()
    {
        return temperature;
    }
    
    public void setTemperature(double temperature)
    {
        this.temperature = temperature;
    }
    
    public int getMaxTokens()
    {
        return maxTokens;
    }
    
    public void setMaxTokens(int maxTokens)
    {
        this.maxTokens = maxTokens;
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
