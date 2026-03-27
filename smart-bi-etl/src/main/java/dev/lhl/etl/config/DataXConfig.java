package dev.lhl.etl.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DataX配置类
 * 配置DataX相关参数
 * 
 * @author smart-bi
 */
@Configuration
@ConfigurationProperties(prefix = "datax")
public class DataXConfig
{
    private static final Logger log = LoggerFactory.getLogger(DataXConfig.class);
    
    /**
     * DataX安装路径
     */
    private String homePath = "/opt/datax";
    
    /**
     * DataX Python脚本路径
     */
    private String pythonPath = "python";
    
    /**
     * 任务执行超时时间（秒）
     */
    private int timeout = 3600;
    
    /**
     * 并发线程数
     */
    private int threadCount = 1;
    
    /**
     * 通道记录数
     */
    private int channelRecordCount = 1000;
    
    public String getHomePath()
    {
        return homePath;
    }
    
    public void setHomePath(String homePath)
    {
        this.homePath = homePath;
    }
    
    public String getPythonPath()
    {
        return pythonPath;
    }
    
    public void setPythonPath(String pythonPath)
    {
        this.pythonPath = pythonPath;
    }
    
    public int getTimeout()
    {
        return timeout;
    }
    
    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }
    
    public int getThreadCount()
    {
        return threadCount;
    }
    
    public void setThreadCount(int threadCount)
    {
        this.threadCount = threadCount;
    }
    
    public int getChannelRecordCount()
    {
        return channelRecordCount;
    }
    
    public void setChannelRecordCount(int channelRecordCount)
    {
        this.channelRecordCount = channelRecordCount;
    }
}
