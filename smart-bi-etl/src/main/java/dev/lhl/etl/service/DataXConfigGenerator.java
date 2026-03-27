package dev.lhl.etl.service;

import dev.lhl.common.utils.EncryptUtils;
import dev.lhl.common.utils.StringUtils;
import dev.lhl.datasource.domain.DataSource;
import dev.lhl.etl.config.LocalDataSourceConfig;
import dev.lhl.etl.domain.EtlTask;
import dev.lhl.datasource.service.IDataSourceService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * DataX任务配置生成器
 * 根据ETL任务配置动态生成DataX任务JSON配置
 * 
 * @author smart-bi
 */
@Service
public class DataXConfigGenerator
{
    private static final Logger log = LoggerFactory.getLogger(DataXConfigGenerator.class);
    
    @Autowired(required = false)
    private IDataSourceService dataSourceService;

    @Autowired(required = false)
    private LocalDataSourceConfig localDataSourceConfig;
    
    /**
     * 生成DataX任务配置JSON
     * 
     * @param etlTask ETL任务
     * @param dataSource 数据源
     * @return DataX任务配置JSON字符串
     */
    public String generateConfig(EtlTask etlTask, DataSource dataSource)
    {
        try
        {
            JSONObject config = new JSONObject();
            
            // 设置Job配置
            JSONObject job = new JSONObject();
            job.put("setting", createSetting(etlTask));
            job.put("content", createContent(etlTask, dataSource));
            
            config.put("job", job);
            
            // 使用 FastJSON2 格式化输出（带缩进）
            String configJson = JSON.toJSONString(config, JSONWriter.Feature.PrettyFormat);
            log.debug("生成DataX配置: taskId={}, config={}", etlTask.getId(), configJson);
            
            return configJson;
        }
        catch (Exception e)
        {
            log.error("生成DataX配置失败: taskId={}", etlTask.getId(), e);
            throw new RuntimeException("生成DataX配置失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建Job设置
     */
    private JSONObject createSetting(EtlTask etlTask)
    {
        JSONObject setting = new JSONObject();
        
        // 速度控制（byte/record 设为 0 表示不限速，避免需额外配置 core.transport.channel.speed.byte）
        JSONObject speed = new JSONObject();
        speed.put("channel", 1); // 通道数
        speed.put("byte", 0); // 0=不限速，否则需配置 core.transport.channel.speed.byte
        speed.put("record", 0); // 0=不限速
        setting.put("speed", speed);
        
        // 错误限制
        JSONObject errorLimit = new JSONObject();
        errorLimit.put("record", 0); // 错误记录数限制（0表示不限制）
        errorLimit.put("percentage", 0.02); // 错误百分比限制（2%）
        setting.put("errorLimit", errorLimit);
        
        return setting;
    }
    
    /**
     * 创建Content配置（Reader和Writer数组）
     */
    private JSONArray createContent(EtlTask etlTask, DataSource dataSource)
    {
        JSONArray content = new JSONArray();
        JSONObject item = new JSONObject();
        
        // Reader配置
        item.put("reader", createReader(etlTask, dataSource));
        
        // Writer配置
        item.put("writer", createWriter(etlTask));
        
        content.add(item);
        return content;
    }
    
    /**
     * 创建Reader配置
     */
    private JSONObject createReader(EtlTask etlTask, DataSource dataSource)
    {
        JSONObject reader = new JSONObject();
        
        // 根据数据源类型选择Reader
        String readerName = getReaderName(dataSource.getSubType());
        reader.put("name", readerName);
        
        // Reader参数
        JSONObject parameter = new JSONObject();
        
        if ("DATABASE".equals(dataSource.getType()))
        {
            // 数据库Reader参数（密码需解密，数据库中存储的是加密后的值）
            String password = decryptPassword(dataSource.getPassword());
            parameter.put("username", dataSource.getUsername());
            parameter.put("password", password != null ? password : "");
            parameter.put("column", getColumns(etlTask));
            parameter.put("connection", createConnection(dataSource, etlTask));
        }
        else if ("API".equals(dataSource.getType()))
        {
            // API Reader参数
            parameter.put("url", dataSource.getUrl());
            parameter.put("method", "GET");
            parameter.put("headers", createApiHeaders(dataSource));
        }
        
        reader.put("parameter", parameter);
        
        return reader;
    }
    
    /**
     * 创建Writer配置
     */
    private JSONObject createWriter(EtlTask etlTask)
    {
        JSONObject writer = new JSONObject();
        
        // 使用MySQL Writer（目标数据库）
        writer.put("name", "mysqlwriter");
        
        // Writer参数
        JSONObject parameter = new JSONObject();
        parameter.put("writeMode", "insert"); // 写入模式
        
        // Writer 写入本服务数据库，从 application-druid.yml 的 spring.datasource.druid.master 获取（密码明文，无需解密）
        String targetJdbcUrl = null;
        String targetUsername = "root";
        String targetPassword = "password";
        String targetHost = "localhost";
        String targetPort = "3306";
        String targetDatabase = "smart_bi";
        
        if (localDataSourceConfig != null && localDataSourceConfig.getUrl() != null)
        {
            targetJdbcUrl = localDataSourceConfig.getUrl();
            targetUsername = localDataSourceConfig.getUsername() != null ? localDataSourceConfig.getUsername() : "root";
            targetPassword = localDataSourceConfig.getPassword() != null ? localDataSourceConfig.getPassword() : "password";
            log.debug("从 spring.datasource.druid.master 获取 Writer 连接信息");
        }
        else if (dataSourceService != null)
        {
            try
            {
                String[] localInfo = dataSourceService.getLocalDatabaseConnectionInfo();
                if (localInfo != null && localInfo.length >= 3)
                {
                    targetJdbcUrl = localInfo[0];
                    targetUsername = localInfo[1];
                    targetPassword = localInfo[2];
                    log.debug("从 DataSource Bean 反射获取 Writer 连接信息");
                }
            }
            catch (Exception e)
            {
                log.debug("从 DataSource 获取连接信息失败: {}", e.getMessage());
            }
        }
        
        // 若均未配置，尝试系统属性
        if (targetJdbcUrl == null)
        {
            targetUsername = System.getProperty("datax.target.username", "root");
            targetPassword = System.getProperty("datax.target.password", "password");
            targetHost = System.getProperty("datax.target.host", "localhost");
            targetPort = System.getProperty("datax.target.port", "3306");
            targetDatabase = System.getProperty("datax.target.database", "smart_bi");
            log.debug("使用系统属性或默认值作为 Writer 目标数据库连接信息");
        }
        
        parameter.put("username", targetUsername);
        parameter.put("password", targetPassword);
        parameter.put("column", getColumns(etlTask));
        parameter.put("connection", createTargetConnection(etlTask, targetJdbcUrl, targetHost, targetPort, targetDatabase));
        
        writer.put("parameter", parameter);
        
        // 明确日志，便于排查 Writer 是否误用 Reader 配置（Writer 应为 smart-ai，Reader 为 ai-doc-agent）
        log.info("DataX Writer 目标库配置: jdbcUrl={}, username={} （来自 spring.datasource.druid.master，需与当前服务使用的数据库一致）",
            targetJdbcUrl != null ? targetJdbcUrl : (targetHost + ":" + targetPort + "/" + targetDatabase),
            targetUsername);
        
        return writer;
    }
    
    /**
     * 解密密码（数据库存储为加密值，DataX需要明文）
     */
    private String decryptPassword(String encryptedPassword)
    {
        if (StringUtils.isEmpty(encryptedPassword))
        {
            return null;
        }
        try
        {
            return EncryptUtils.decrypt(encryptedPassword);
        }
        catch (Exception e)
        {
            log.warn("密码解密失败，可能是明文存储: {}", e.getMessage());
            return encryptedPassword;
        }
    }
    
    /**
     * 获取Reader名称
     */
    private String getReaderName(String subType)
    {
        return switch (subType.toUpperCase())
        {
            case "MYSQL" -> "mysqlreader";
            case "POSTGRESQL" -> "postgresqlreader";
            case "SQLSERVER" -> "sqlserverreader";
            case "ORACLE" -> "oraclereader";
            case "REST" -> "httpreader";
            default -> throw new IllegalArgumentException("不支持的数据源类型: " + subType);
        };
    }
    
    /**
     * 获取列配置
     */
    private String[] getColumns(EtlTask etlTask)
    {
        try
        {
            String sourceConfig = etlTask.getSourceConfig();
            if (sourceConfig != null && !sourceConfig.trim().isEmpty())
            {
                JSONObject config = JSON.parseObject(sourceConfig);
                
                // 尝试从配置中获取列信息
                if (config.containsKey("columns"))
                {
                    JSONArray columnsArray = config.getJSONArray("columns");
                    if (columnsArray != null && !columnsArray.isEmpty())
                    {
                        String[] columns = new String[columnsArray.size()];
                        for (int i = 0; i < columnsArray.size(); i++)
                        {
                            columns[i] = columnsArray.getString(i);
                        }
                        return columns;
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.warn("解析列配置失败，使用默认值: taskId={}", etlTask.getId(), e);
        }
        
        // 默认返回所有列
        return new String[]{"*"};
    }
    
    /**
     * 创建连接配置
     */
    private JSONObject[] createConnection(DataSource dataSource, EtlTask etlTask)
    {
        JSONObject connection = new JSONObject();
        
        if ("DATABASE".equals(dataSource.getType()))
        {
            String jdbcUrl = buildJdbcUrl(dataSource);
            JSONArray jdbcUrls = new JSONArray();
            jdbcUrls.add(jdbcUrl);
            connection.put("jdbcUrl", jdbcUrls);
            
            JSONArray tables = new JSONArray();
            
            // 从sourceConfig解析表名
            try
            {
                String sourceConfig = etlTask.getSourceConfig();
                if (sourceConfig != null && !sourceConfig.trim().isEmpty())
                {
                    JSONObject config = JSON.parseObject(sourceConfig);
                    
                    // 如果sourceType是TABLE，从配置中获取表名
                    if ("TABLE".equals(etlTask.getSourceType()) && config.containsKey("tableName"))
                    {
                        String tableName = config.getString("tableName");
                        if (tableName != null && !tableName.trim().isEmpty())
                        {
                            tables.add(tableName);
                        }
                        else
                        {
                            tables.add(etlTask.getTargetTable());
                        }
                    }
                    else
                    {
                        tables.add(etlTask.getTargetTable());
                    }
                }
                else
                {
                    tables.add(etlTask.getTargetTable());
                }
            }
            catch (Exception e)
            {
                log.warn("解析表名失败，使用目标表名: taskId={}", etlTask.getId(), e);
                tables.add(etlTask.getTargetTable());
            }
            
            connection.put("table", tables);
        }
        
        return new JSONObject[]{connection};
    }
    
    /**
     * 创建目标连接配置
     * @param etlTask ETL任务
     * @param jdbcUrl 完整 JDBC URL（来自 yaml 时使用）；为 null 时由 host/port/database 拼接
     */
    private JSONObject[] createTargetConnection(EtlTask etlTask, String jdbcUrl, String host, String port, String database)
    {
        JSONObject connection = new JSONObject();
        
        String finalJdbcUrl;
        if (StringUtils.isNotEmpty(jdbcUrl))
        {
            // 确保 useSSL=false，避免部分环境 SSL 导致 Communications link failure
            finalJdbcUrl = ensureUseSslFalse(jdbcUrl);
        }
        else
        {
            finalJdbcUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&yearIsDateType=false&zeroDateTimeBehavior=convertToNull", host, port, database);
        }
        connection.put("jdbcUrl", finalJdbcUrl);
        
        JSONArray tables = new JSONArray();
        tables.add(etlTask.getTargetTable());
        connection.put("table", tables);
        
        return new JSONObject[]{connection};
    }
    
    /**
     * 确保 JDBC URL 使用 useSSL=false 和 allowPublicKeyRetrieval=true，避免 Communications link failure
     */
    private String ensureUseSslFalse(String url)
    {
        if (url == null) return url;
        url = url.replace("useSSL=true", "useSSL=false");
        if (!url.contains("useSSL="))
        {
            url = url + (url.contains("?") ? "&" : "?") + "useSSL=false";
        }
        if (!url.contains("allowPublicKeyRetrieval="))
        {
            url = url + (url.contains("?") ? "&" : "?") + "allowPublicKeyRetrieval=true";
        }
        return url;
    }

    /**
     * 构建JDBC URL
     */
    private String buildJdbcUrl(DataSource dataSource)
    {
        return switch (dataSource.getSubType().toUpperCase())
        {
            case "MYSQL" -> String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", 
                dataSource.getHost(), dataSource.getPort(), dataSource.getDatabaseName());
            case "POSTGRESQL" -> String.format("jdbc:postgresql://%s:%d/%s", 
                dataSource.getHost(), dataSource.getPort(), dataSource.getDatabaseName());
            case "SQLSERVER" -> String.format("jdbc:sqlserver://%s:%d;databaseName=%s", 
                dataSource.getHost(), dataSource.getPort(), dataSource.getDatabaseName());
            case "ORACLE" -> String.format("jdbc:oracle:thin:@%s:%d:%s", 
                dataSource.getHost(), dataSource.getPort(), dataSource.getDatabaseName());
            default -> throw new IllegalArgumentException("不支持的数据源类型: " + dataSource.getSubType());
        };
    }
    
    /**
     * 创建API请求头
     */
    private JSONObject createApiHeaders(DataSource dataSource)
    {
        JSONObject headers = new JSONObject();
        
        try
        {
            // 从authConfig解析API请求头
            String authConfig = dataSource.getAuthConfig();
            if (authConfig != null && !authConfig.trim().isEmpty())
            {
                JSONObject config = JSON.parseObject(authConfig);
                
                String authType = dataSource.getAuthType();
                if ("API_KEY".equals(authType))
                {
                    // API Key认证
                    String apiKey = config.getString("apiKey");
                    String headerName = config.getString("headerName");
                    if (headerName == null || headerName.trim().isEmpty())
                    {
                        headerName = "X-API-Key"; // 默认请求头名称
                    }
                    if (apiKey != null && !apiKey.trim().isEmpty())
                    {
                        headers.put(headerName, apiKey);
                    }
                }
                else if ("BASIC_AUTH".equals(authType))
                {
                    // Basic认证
                    String username = config.getString("username");
                    String password = config.getString("password");
                    if (username != null && password != null)
                    {
                        String auth = username + ":" + password;
                        String encodedAuth = java.util.Base64.getEncoder()
                            .encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        headers.put("Authorization", "Basic " + encodedAuth);
                    }
                }
                else if ("BEARER_TOKEN".equals(authType) || "OAUTH2".equals(authType))
                {
                    // Bearer Token认证
                    String token = config.getString("token");
                    if (token != null && !token.trim().isEmpty())
                    {
                        headers.put("Authorization", "Bearer " + token);
                    }
                }
                
                // 添加其他自定义请求头
                if (config.containsKey("headers"))
                {
                    JSONObject customHeaders = config.getJSONObject("headers");
                    if (customHeaders != null)
                    {
                        headers.putAll(customHeaders);
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.warn("解析API请求头失败: datasourceId={}", dataSource.getId(), e);
        }
        
        // 添加默认请求头
        if (!headers.containsKey("Content-Type"))
        {
            headers.put("Content-Type", "application/json");
        }
        
        return headers;
    }
}
