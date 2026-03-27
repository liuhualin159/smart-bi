package dev.lhl.datasource.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import dev.lhl.common.annotation.Excel;
import dev.lhl.common.core.domain.BaseEntity;

/**
 * 数据源对象 bi_datasource
 * 
 * @author smart-bi
 */
public class DataSource extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 数据源ID */
    private Long id;

    /** 数据源名称 */
    @Excel(name = "数据源名称")
    private String name;

    /** 数据源类型（DATABASE/API） */
    @Excel(name = "数据源类型", readConverterExp = "DATABASE=数据库,API=API接口")
    private String type;

    /** 子类型（MySQL/PostgreSQL/SQLServer/Oracle/REST） */
    @Excel(name = "子类型")
    private String subType;

    /** 主机地址 */
    @Excel(name = "主机地址")
    private String host;

    /** 端口号 */
    @Excel(name = "端口号")
    private Integer port;

    /** 数据库名 */
    @Excel(name = "数据库名")
    private String databaseName;

    /** 连接URL（API类型时） */
    @Excel(name = "连接URL")
    private String url;

    /** 用户名（加密存储） */
    private String username;

    /** 密码（加密存储） */
    private String password;

    /** 认证类型（USERNAME_PASSWORD/API_KEY/BASIC_AUTH/OAUTH2） */
    @Excel(name = "认证类型")
    private String authType;

    /** 认证配置（JSON格式，加密存储） */
    private String authConfig;

    /** 状态（ACTIVE/INACTIVE） */
    @Excel(name = "状态", readConverterExp = "ACTIVE=启用,INACTIVE=停用")
    private String status;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getType()
    {
        return type;
    }

    public void setSubType(String subType)
    {
        this.subType = subType;
    }

    public String getSubType()
    {
        return subType;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public String getHost()
    {
        return host;
    }

    public void setPort(Integer port)
    {
        this.port = port;
    }

    public Integer getPort()
    {
        return port;
    }

    public void setDatabaseName(String databaseName)
    {
        this.databaseName = databaseName;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getUsername()
    {
        return username;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getPassword()
    {
        return password;
    }

    public void setAuthType(String authType)
    {
        this.authType = authType;
    }

    public String getAuthType()
    {
        return authType;
    }

    public void setAuthConfig(String authConfig)
    {
        this.authConfig = authConfig;
    }

    public String getAuthConfig()
    {
        return authConfig;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("type", getType())
            .append("subType", getSubType())
            .append("host", getHost())
            .append("port", getPort())
            .append("databaseName", getDatabaseName())
            .append("url", getUrl())
            .append("username", getUsername())
            .append("authType", getAuthType())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
