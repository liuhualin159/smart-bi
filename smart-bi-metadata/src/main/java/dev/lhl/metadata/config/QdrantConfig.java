package dev.lhl.metadata.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Qdrant配置类
 * 配置Qdrant向量数据库连接信息
 * 
 * @author smart-bi
 */
@Configuration
@ConfigurationProperties(prefix = "qdrant")
public class QdrantConfig
{
    private static final Logger log = LoggerFactory.getLogger(QdrantConfig.class);
    
    /**
     * Qdrant服务器地址
     */
    private String host = "localhost";
    
    /**
     * Qdrant服务器端口
     */
    private int port = 6333;
    
    /**
     * API密钥（可选）
     */
    private String apiKey;
    
    /**
     * 是否使用HTTPS
     */
    private boolean useHttps = false;
    
    /**
     * 连接超时时间（秒）
     */
    private int timeout = 30;
    
    /**
     * 集合名称
     */
    private String collectionName = "smart_bi_metadata";
    
    /**
     * 向量维度
     * 默认1536（适用于text-embedding-ada-002、text-embedding-v1等模型）
     * 如果使用其他模型，需要根据模型输出维度调整
     */
    private int vectorSize = 1536;
    
    public String getHost()
    {
        return host;
    }
    
    public void setHost(String host)
    {
        this.host = host;
    }
    
    public int getPort()
    {
        return port;
    }
    
    public void setPort(int port)
    {
        this.port = port;
    }
    
    public String getApiKey()
    {
        return apiKey;
    }
    
    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }
    
    public boolean isUseHttps()
    {
        return useHttps;
    }
    
    public void setUseHttps(boolean useHttps)
    {
        this.useHttps = useHttps;
    }
    
    public int getTimeout()
    {
        return timeout;
    }
    
    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }
    
    public String getCollectionName()
    {
        return collectionName;
    }
    
    public void setCollectionName(String collectionName)
    {
        this.collectionName = collectionName;
    }
    
    public int getVectorSize()
    {
        return vectorSize;
    }
    
    public void setVectorSize(int vectorSize)
    {
        this.vectorSize = vectorSize;
    }
    
    /**
     * 获取Qdrant连接URL
     */
    public String getUrl()
    {
        String protocol = useHttps ? "https" : "http";
        return String.format("%s://%s:%d", protocol, host, port);
    }
}
