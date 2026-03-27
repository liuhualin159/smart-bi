package dev.lhl.etl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地数据源配置（从 application-druid.yml 的 master 读取）
 * 用于 DataX Writer 写入目标库，密码为 yaml 明文
 */
@Component
@ConfigurationProperties(prefix = "spring.datasource.druid.master")
public class LocalDataSourceConfig
{
    private String url;
    private String username;
    private String password;

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }
}
