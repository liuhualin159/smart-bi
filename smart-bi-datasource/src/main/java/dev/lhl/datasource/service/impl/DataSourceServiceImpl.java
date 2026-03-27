package dev.lhl.datasource.service.impl;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import dev.lhl.common.utils.DateUtils;
import dev.lhl.common.utils.EncryptUtils;
import dev.lhl.common.utils.SecurityUtils;
import dev.lhl.common.utils.StringUtils;
import dev.lhl.common.exception.ServiceException;
import dev.lhl.datasource.domain.DataSource;
import dev.lhl.datasource.mapper.DataSourceMapper;
import dev.lhl.datasource.service.IDataSourceService;

/**
 * 数据源Service业务层处理
 * 
 * @author smart-bi
 */
@Service
public class DataSourceServiceImpl implements IDataSourceService
{
    private static final Logger log = LoggerFactory.getLogger(DataSourceServiceImpl.class);

    @Autowired
    private DataSourceMapper dataSourceMapper;

    @Autowired(required = false)
    @Qualifier("masterDataSource")
    private javax.sql.DataSource localDataSource;

    /**
     * 查询数据源
     * 
     * @param id 数据源ID
     * @return 数据源
     */
    @Override
    public DataSource selectDataSourceById(Long id)
    {
        DataSource dataSource = dataSourceMapper.selectDataSourceById(id);
        // 解密密码（仅用于显示，不返回给前端）
        if (dataSource != null && StringUtils.isNotEmpty(dataSource.getPassword()))
        {
            try
            {
                String decryptedPassword = EncryptUtils.decrypt(dataSource.getPassword());
                // 注意：实际返回时不应包含密码，这里仅用于内部处理
                log.debug("解密数据源密码: id={}", id);
            }
            catch (Exception e)
            {
                log.warn("解密数据源密码失败: id={}", id, e);
            }
        }
        return dataSource;
    }

    /**
     * 查询数据源列表
     * 
     * @param dataSource 数据源
     * @return 数据源
     */
    @Override
    public List<dev.lhl.datasource.domain.DataSource> selectDataSourceList(dev.lhl.datasource.domain.DataSource dataSource)
    {
        List<dev.lhl.datasource.domain.DataSource> list = dataSourceMapper.selectDataSourceList(dataSource);
        // 清除敏感信息
        list.forEach(ds -> {
            ds.setPassword(null);
            ds.setUsername(null);
            ds.setAuthConfig(null);
        });
        return list;
    }

    /**
     * 新增数据源
     * 
     * @param dataSource 数据源
     * @return 结果
     */
    @Override
    public int insertDataSource(dev.lhl.datasource.domain.DataSource dataSource)
    {
        try
        {
            // 参数验证
            validateDataSource(dataSource);
            
            // 设置创建信息
            dataSource.setCreateBy(SecurityUtils.getUsername());
            dataSource.setCreateTime(DateUtils.getNowDate());
            
            // 加密存储密码和认证配置
            if (StringUtils.isNotEmpty(dataSource.getPassword()))
            {
                dataSource.setPassword(EncryptUtils.encrypt(dataSource.getPassword()));
            }
            if (StringUtils.isNotEmpty(dataSource.getAuthConfig()))
            {
                dataSource.setAuthConfig(EncryptUtils.encrypt(dataSource.getAuthConfig()));
            }
            
            // 设置默认状态
            if (StringUtils.isEmpty(dataSource.getStatus()))
            {
                dataSource.setStatus("ACTIVE");
            }
            
            log.info("新增数据源: name={}, type={}", dataSource.getName(), dataSource.getType());
            return dataSourceMapper.insertDataSource(dataSource);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("新增数据源失败: name={}", dataSource.getName(), e);
            throw new ServiceException("新增数据源失败: " + e.getMessage());
        }
    }

    /**
     * 修改数据源
     * 
     * @param dataSource 数据源
     * @return 结果
     */
    @Override
    public int updateDataSource(dev.lhl.datasource.domain.DataSource dataSource)
    {
        try
        {
            // 参数验证
            validateDataSource(dataSource);
            
            // 设置更新信息
            dataSource.setUpdateBy(SecurityUtils.getUsername());
            dataSource.setUpdateTime(DateUtils.getNowDate());
            
            // 如果密码不为空，则加密存储
            if (StringUtils.isNotEmpty(dataSource.getPassword()))
            {
                dataSource.setPassword(EncryptUtils.encrypt(dataSource.getPassword()));
            }
            else
            {
                // 如果密码为空，保持原密码不变
                dev.lhl.datasource.domain.DataSource existing = dataSourceMapper.selectDataSourceById(dataSource.getId());
                if (existing != null)
                {
                    dataSource.setPassword(existing.getPassword());
                }
            }
            
            // 如果用户名为空，保持原用户名不变（编辑时前端可能不传）
            if (StringUtils.isEmpty(dataSource.getUsername()))
            {
                dev.lhl.datasource.domain.DataSource existing = dataSourceMapper.selectDataSourceById(dataSource.getId());
                if (existing != null)
                {
                    dataSource.setUsername(existing.getUsername());
                }
            }
            
            // 如果认证配置不为空，则加密存储
            if (StringUtils.isNotEmpty(dataSource.getAuthConfig()))
            {
                dataSource.setAuthConfig(EncryptUtils.encrypt(dataSource.getAuthConfig()));
            }
            else
            {
                // 如果认证配置为空，保持原配置不变
                dev.lhl.datasource.domain.DataSource existing = dataSourceMapper.selectDataSourceById(dataSource.getId());
                if (existing != null)
                {
                    dataSource.setAuthConfig(existing.getAuthConfig());
                }
            }
            
            log.info("修改数据源: id={}, name={}", dataSource.getId(), dataSource.getName());
            return dataSourceMapper.updateDataSource(dataSource);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("修改数据源失败: id={}", dataSource.getId(), e);
            throw new ServiceException("修改数据源失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除数据源
     * 
     * @param ids 需要删除的数据源ID
     * @return 结果
     */
    @Override
    public int deleteDataSourceByIds(Long[] ids)
    {
        log.info("批量删除数据源: ids={}", ids);
        return dataSourceMapper.deleteDataSourceByIds(ids);
    }

    /**
     * 删除数据源信息
     * 
     * @param id 数据源ID
     * @return 结果
     */
    @Override
    public int deleteDataSourceById(Long id)
    {
        log.info("删除数据源: id={}", id);
        return dataSourceMapper.deleteDataSourceById(id);
    }

    /**
     * 测试数据源连接
     * 
     * @param dataSource 数据源
     * @return true表示连接成功
     */
    @Override
    public boolean testConnection(dev.lhl.datasource.domain.DataSource dataSource)
    {
        try
        {
            log.info("测试数据源连接: name={}, type={}", dataSource.getName(), dataSource.getType());
            
            if ("DATABASE".equals(dataSource.getType()))
            {
                return testDatabaseConnection(dataSource);
            }
            else if ("API".equals(dataSource.getType()))
            {
                return testApiConnection(dataSource);
            }
            else
            {
                throw new ServiceException("不支持的数据源类型: " + dataSource.getType());
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("测试数据源连接失败: name={}", dataSource.getName(), e);
            throw new ServiceException("测试连接失败: " + e.getMessage());
        }
    }

    /**
     * 测试数据库连接
     */
    private boolean testDatabaseConnection(dev.lhl.datasource.domain.DataSource dataSource)
    {
        Connection conn = null;
        try
        {
            // 解密密码
            String password = dataSource.getPassword();
            if (StringUtils.isNotEmpty(password))
            {
            try
            {
                password = EncryptUtils.decrypt(password);
            }
            catch (Exception e)
            {
                // 如果解密失败，可能是明文密码（测试场景）
                log.debug("密码解密失败，使用明文密码: {}", e.getMessage());
            }
            }
            
            // 构建JDBC URL
            String jdbcUrl = buildJdbcUrl(dataSource);
            
            // 加载驱动
            String driverClass = getDriverClass(dataSource.getSubType());
            Class.forName(driverClass);
            
            // 建立连接
            conn = DriverManager.getConnection(jdbcUrl, dataSource.getUsername(), password);
            
            // 测试连接
            boolean isValid = conn.isValid(5); // 5秒超时
            
            log.info("数据库连接测试成功: name={}, type={}", dataSource.getName(), dataSource.getSubType());
            return isValid;
        }
        catch (ClassNotFoundException e)
        {
            log.error("数据库驱动未找到: subType={}", dataSource.getSubType(), e);
            throw new ServiceException("数据库驱动未找到: " + dataSource.getSubType());
        }
        catch (java.sql.SQLException e)
        {
            log.error("数据库连接失败: name={}", dataSource.getName(), e);
            String errorMsg = e.getMessage();
            if (errorMsg.contains("Access denied") || errorMsg.contains("authentication"))
            {
                throw new ServiceException("认证失败：用户名或密码错误");
            }
            else if (errorMsg.contains("Communications link failure") || errorMsg.contains("Connection refused"))
            {
                throw new ServiceException("网络连接失败：无法连接到数据库服务器");
            }
            else
            {
                throw new ServiceException("连接失败: " + errorMsg);
            }
        }
        catch (Exception e)
        {
            log.error("数据库连接测试异常: name={}", dataSource.getName(), e);
            throw new ServiceException("连接测试失败: " + e.getMessage());
        }
        finally
        {
            if (conn != null)
            {
                try
                {
                    conn.close();
                }
                catch (java.sql.SQLException e)
                {
                    log.warn("关闭数据库连接失败", e);
                }
            }
        }
    }

    /**
     * 测试API连接
     */
    private boolean testApiConnection(dev.lhl.datasource.domain.DataSource dataSource)
    {
        try
        {
            log.info("API连接测试: name={}, url={}", dataSource.getName(), dataSource.getUrl());
            
            if (StringUtils.isEmpty(dataSource.getUrl()))
            {
                throw new ServiceException("API URL不能为空");
            }
            
            // 1. 构建HTTP请求
            java.net.URL url = new java.net.URL(dataSource.getUrl());
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            
            // 设置请求方法和超时
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5秒连接超时
            connection.setReadTimeout(10000);   // 10秒读取超时
            
            // 2. 处理认证
            if (StringUtils.isNotEmpty(dataSource.getAuthType()))
            {
                applyAuth(connection, dataSource);
            }
            
            // 3. 发送请求
            int responseCode = connection.getResponseCode();
            
            // 4. 检查响应状态码（2xx表示成功）
            boolean success = responseCode >= 200 && responseCode < 300;
            
            if (success)
            {
                log.info("API连接测试成功: name={}, url={}, statusCode={}", 
                    dataSource.getName(), dataSource.getUrl(), responseCode);
            }
            else
            {
                log.warn("API连接测试失败: name={}, url={}, statusCode={}", 
                    dataSource.getName(), dataSource.getUrl(), responseCode);
            }
            
            connection.disconnect();
            return success;
        }
        catch (java.net.MalformedURLException e)
        {
            log.error("API URL格式错误: url={}", dataSource.getUrl(), e);
            throw new ServiceException("API URL格式错误: " + e.getMessage());
        }
        catch (java.net.SocketTimeoutException e)
        {
            log.error("API连接超时: url={}", dataSource.getUrl(), e);
            throw new ServiceException("API连接超时，请检查网络连接或URL是否正确");
        }
        catch (java.io.IOException e)
        {
            log.error("API连接失败: url={}", dataSource.getUrl(), e);
            throw new ServiceException("API连接失败: " + e.getMessage());
        }
        catch (Exception e)
        {
            log.error("API连接测试失败: name={}", dataSource.getName(), e);
            throw new ServiceException("API连接测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 应用认证到HTTP连接
     */
    private void applyAuth(java.net.HttpURLConnection connection, dev.lhl.datasource.domain.DataSource dataSource)
    {
        try
        {
            String authType = dataSource.getAuthType();
            String authConfig = dataSource.getAuthConfig();
            
            if (StringUtils.isEmpty(authConfig))
            {
                return;
            }
            
            // 解析认证配置（JSON格式）
            com.alibaba.fastjson2.JSONObject config = com.alibaba.fastjson2.JSON.parseObject(authConfig);
            
            switch (authType.toUpperCase())
            {
                case "API_KEY":
                    // API Key认证：添加到请求头
                    String apiKey = config.getString("apiKey");
                    String apiKeyHeader = config.getString("headerName");
                    if (StringUtils.isEmpty(apiKeyHeader))
                    {
                        apiKeyHeader = "X-API-Key"; // 默认请求头名称
                    }
                    if (StringUtils.isNotEmpty(apiKey))
                    {
                        connection.setRequestProperty(apiKeyHeader, apiKey);
                    }
                    break;
                    
                case "BASIC_AUTH":
                    // Basic认证
                    String username = config.getString("username");
                    String password = config.getString("password");
                    if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password))
                    {
                        String auth = username + ":" + password;
                        String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                    }
                    break;
                    
                case "BEARER_TOKEN":
                    // Bearer Token认证
                    String token = config.getString("token");
                    if (StringUtils.isNotEmpty(token))
                    {
                        connection.setRequestProperty("Authorization", "Bearer " + token);
                    }
                    break;
                    
                default:
                    log.warn("不支持的认证类型: authType={}", authType);
                    break;
            }
        }
        catch (Exception e)
        {
            log.warn("应用认证配置失败，继续测试连接: authType={}", dataSource.getAuthType(), e);
        }
    }

    /**
     * 构建JDBC URL
     */
    private String buildJdbcUrl(dev.lhl.datasource.domain.DataSource dataSource)
    {
        return switch (dataSource.getSubType().toUpperCase())
        {
            case "MYSQL" -> String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
                dataSource.getHost(), dataSource.getPort(), dataSource.getDatabaseName());
            case "POSTGRESQL" -> String.format("jdbc:postgresql://%s:%d/%s",
                dataSource.getHost(), dataSource.getPort(), dataSource.getDatabaseName());
            case "SQLSERVER" -> String.format("jdbc:sqlserver://%s:%d;databaseName=%s",
                dataSource.getHost(), dataSource.getPort(), dataSource.getDatabaseName());
            case "ORACLE" -> String.format("jdbc:oracle:thin:@%s:%d:%s",
                dataSource.getHost(), dataSource.getPort(), dataSource.getDatabaseName());
            default -> throw new ServiceException("不支持的数据库类型: " + dataSource.getSubType());
        };
    }

    /**
     * 获取驱动类名
     */
    private String getDriverClass(String subType)
    {
        return switch (subType.toUpperCase())
        {
            case "MYSQL" -> "com.mysql.cj.jdbc.Driver";
            case "POSTGRESQL" -> "org.postgresql.Driver";
            case "SQLSERVER" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "ORACLE" -> "oracle.jdbc.driver.OracleDriver";
            default -> throw new ServiceException("不支持的数据库类型: " + subType);
        };
    }

    /**
     * 验证数据源参数
     */
    private void validateDataSource(dev.lhl.datasource.domain.DataSource dataSource)
    {
        if (StringUtils.isEmpty(dataSource.getName()))
        {
            throw new ServiceException("数据源名称不能为空");
        }
        if (dataSource.getName().length() > 100)
        {
            throw new ServiceException("数据源名称长度不能超过100个字符");
        }
        if (StringUtils.isEmpty(dataSource.getType()))
        {
            throw new ServiceException("数据源类型不能为空");
        }
        if (!"DATABASE".equals(dataSource.getType()) && !"API".equals(dataSource.getType()))
        {
            throw new ServiceException("数据源类型必须是DATABASE或API");
        }
        if ("DATABASE".equals(dataSource.getType()))
        {
            if (StringUtils.isEmpty(dataSource.getHost()))
            {
                throw new ServiceException("数据库主机地址不能为空");
            }
            if (dataSource.getPort() == null || dataSource.getPort() <= 0)
            {
                throw new ServiceException("数据库端口号无效");
            }
            if (StringUtils.isEmpty(dataSource.getDatabaseName()))
            {
                throw new ServiceException("数据库名不能为空");
            }
        }
        else if ("API".equals(dataSource.getType()))
        {
            if (StringUtils.isEmpty(dataSource.getUrl()))
            {
                throw new ServiceException("API连接URL不能为空");
            }
        }
    }

    /**
     * 查询数据源的表列表
     */
    @Override
    public List<Map<String, Object>> getTableList(Long dataSourceId)
    {
        DataSource dataSource = selectDataSourceById(dataSourceId);
        if (dataSource == null)
        {
            throw new ServiceException("数据源不存在: id=" + dataSourceId);
        }
        if (!"DATABASE".equals(dataSource.getType()))
        {
            throw new ServiceException("只有数据库类型的数据源支持查询表列表");
        }

        Connection conn = null;
        try
        {
            // 获取数据库连接
            conn = getConnection(dataSource);
            DatabaseMetaData metaData = conn.getMetaData();
            
            List<Map<String, Object>> tableList = new ArrayList<>();
            
            // 根据数据库类型查询表列表
            String catalog = null;
            String schema = null;
            String[] types = {"TABLE", "VIEW"};
            
            // MySQL使用catalog（数据库名），PostgreSQL使用schema
            if ("MYSQL".equalsIgnoreCase(dataSource.getSubType()))
            {
                catalog = dataSource.getDatabaseName();
            }
            else if ("POSTGRESQL".equalsIgnoreCase(dataSource.getSubType()))
            {
                schema = "public";
            }
            
            ResultSet rs = metaData.getTables(catalog, schema, null, types);
            while (rs.next())
            {
                Map<String, Object> table = new HashMap<>();
                table.put("tableName", rs.getString("TABLE_NAME"));
                table.put("tableComment", rs.getString("REMARKS"));
                table.put("tableType", rs.getString("TABLE_TYPE"));
                tableList.add(table);
            }
            rs.close();
            
            log.info("查询表列表成功: dataSourceId={}, tableCount={}", dataSourceId, tableList.size());
            return tableList;
        }
        catch (Exception e)
        {
            log.error("查询表列表失败: dataSourceId={}", dataSourceId, e);
            throw new ServiceException("查询表列表失败: " + e.getMessage());
        }
        finally
        {
            if (conn != null)
            {
                try
                {
                    conn.close();
                }
                catch (Exception e)
                {
                    log.warn("关闭数据库连接失败", e);
                }
            }
        }
    }

    /**
     * 查询指定表的字段列表
     */
    @Override
    public List<Map<String, Object>> getColumnList(Long dataSourceId, String tableName)
    {
        if (StringUtils.isEmpty(tableName))
        {
            throw new ServiceException("表名不能为空");
        }

        dev.lhl.datasource.domain.DataSource dataSource = selectDataSourceById(dataSourceId);
        if (dataSource == null)
        {
            throw new ServiceException("数据源不存在: id=" + dataSourceId);
        }
        if (!"DATABASE".equals(dataSource.getType()))
        {
            throw new ServiceException("只有数据库类型的数据源支持查询字段列表");
        }

        Connection conn = null;
        try
        {
            // 获取数据库连接
            conn = getConnection(dataSource);
            DatabaseMetaData metaData = conn.getMetaData();
            
            List<Map<String, Object>> columnList = new ArrayList<>();
            
            // 根据数据库类型查询字段列表
            String catalog = null;
            String schema = null;
            
            if ("MYSQL".equalsIgnoreCase(dataSource.getSubType()))
            {
                catalog = dataSource.getDatabaseName();
            }
            else if ("POSTGRESQL".equalsIgnoreCase(dataSource.getSubType()))
            {
                schema = "public";
            }
            
            ResultSet rs = metaData.getColumns(catalog, schema, tableName, null);
            while (rs.next())
            {
                Map<String, Object> column = new HashMap<>();
                column.put("columnName", rs.getString("COLUMN_NAME"));
                column.put("dataType", rs.getString("TYPE_NAME"));
                column.put("columnSize", rs.getInt("COLUMN_SIZE"));
                column.put("decimalDigits", rs.getInt("DECIMAL_DIGITS"));
                column.put("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.put("columnComment", rs.getString("REMARKS"));
                column.put("columnDefault", rs.getString("COLUMN_DEF"));
                columnList.add(column);
            }
            rs.close();
            
            log.info("查询字段列表成功: dataSourceId={}, tableName={}, columnCount={}", dataSourceId, tableName, columnList.size());
            return columnList;
        }
        catch (Exception e)
        {
            log.error("查询字段列表失败: dataSourceId={}, tableName={}", dataSourceId, tableName, e);
            throw new ServiceException("查询字段列表失败: " + e.getMessage());
        }
        finally
        {
            if (conn != null)
            {
                try
                {
                    conn.close();
                }
                catch (Exception e)
                {
                    log.warn("关闭数据库连接失败", e);
                }
            }
        }
    }

    /**
     * 获取数据库连接
     */
    private Connection getConnection(DataSource dataSource) throws Exception
    {
        // 解密密码
        String password = dataSource.getPassword();
        if (StringUtils.isNotEmpty(password))
        {
            try
            {
                password = EncryptUtils.decrypt(password);
            }
            catch (Exception e)
            {
                log.debug("密码解密失败，使用明文密码");
            }
        }
        
        // 构建JDBC URL
        String jdbcUrl = buildJdbcUrl(dataSource);
        
        // 加载驱动
        String driverClass = getDriverClass(dataSource.getSubType());
        Class.forName(driverClass);
        
        // 建立连接
        Connection conn = DriverManager.getConnection(jdbcUrl, dataSource.getUsername(), password);
        return conn;
    }

    /**
     * 查询本地数据库的表列表
     */
    @Override
    public List<Map<String, Object>> getLocalTableList()
    {
        if (localDataSource == null)
        {
            throw new ServiceException("本地数据源未配置");
        }

        Connection conn = null;
        try
        {
            conn = localDataSource.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            
            List<Map<String, Object>> tableList = new ArrayList<>();
            
            // 获取当前数据库名
            String catalog = conn.getCatalog();
            String[] types = {"TABLE", "VIEW"};
            
            ResultSet rs = metaData.getTables(catalog, null, null, types);
            while (rs.next())
            {
                Map<String, Object> table = new HashMap<>();
                table.put("tableName", rs.getString("TABLE_NAME"));
                table.put("tableComment", rs.getString("REMARKS"));
                table.put("tableType", rs.getString("TABLE_TYPE"));
                tableList.add(table);
            }
            rs.close();
            
            log.info("查询本地表列表成功: tableCount={}", tableList.size());
            return tableList;
        }
        catch (Exception e)
        {
            log.error("查询本地表列表失败", e);
            throw new ServiceException("查询本地表列表失败: " + e.getMessage());
        }
        finally
        {
            if (conn != null)
            {
                try
                {
                    conn.close();
                }
                catch (Exception e)
                {
                    log.warn("关闭数据库连接失败", e);
                }
            }
        }
    }

    /**
     * 查询本地数据库指定表的字段列表
     */
    @Override
    public List<Map<String, Object>> getLocalColumnList(String tableName)
    {
        if (StringUtils.isEmpty(tableName))
        {
            throw new ServiceException("表名不能为空");
        }
        if (localDataSource == null)
        {
            throw new ServiceException("本地数据源未配置");
        }

        Connection conn = null;
        try
        {
            conn = localDataSource.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();

            List<Map<String, Object>> columnList = new ArrayList<>();
            ResultSet rs = metaData.getColumns(catalog, null, tableName, null);
            while (rs.next())
            {
                Map<String, Object> column = new HashMap<>();
                column.put("columnName", rs.getString("COLUMN_NAME"));
                column.put("dataType", rs.getString("TYPE_NAME"));
                column.put("columnSize", rs.getInt("COLUMN_SIZE"));
                column.put("decimalDigits", rs.getInt("DECIMAL_DIGITS"));
                column.put("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.put("columnComment", rs.getString("REMARKS"));
                column.put("columnDefault", rs.getString("COLUMN_DEF"));
                columnList.add(column);
            }
            rs.close();

            log.info("查询本地表字段列表成功: tableName={}, columnCount={}", tableName, columnList.size());
            return columnList;
        }
        catch (Exception e)
        {
            log.error("查询本地表字段列表失败: tableName={}", tableName, e);
            throw new ServiceException("查询本地表字段列表失败: " + e.getMessage());
        }
        finally
        {
            if (conn != null)
            {
                try
                {
                    conn.close();
                }
                catch (Exception e)
                {
                    log.warn("关闭数据库连接失败", e);
                }
            }
        }
    }

    /**
     * 根据源表结构自动创建目标表
     */
    @Override
    public boolean createTargetTableFromSource(Long sourceDataSourceId, String sourceTableName, String targetTableName)
    {
        if (StringUtils.isEmpty(sourceTableName) || StringUtils.isEmpty(targetTableName))
        {
            throw new ServiceException("源表名和目标表名不能为空");
        }

        if (localDataSource == null)
        {
            throw new ServiceException("本地数据源未配置");
        }

        dev.lhl.datasource.domain.DataSource sourceDataSource = selectDataSourceById(sourceDataSourceId);
        if (sourceDataSource == null)
        {
            throw new ServiceException("源数据源不存在: id=" + sourceDataSourceId);
        }
        if (!"DATABASE".equals(sourceDataSource.getType()))
        {
            throw new ServiceException("只有数据库类型的数据源支持自动建表");
        }

        Connection sourceConn = null;
        Connection targetConn = null;
        Statement stmt = null;
        
        try
        {
            // 获取源表结构
            sourceConn = getConnection(sourceDataSource);
            DatabaseMetaData sourceMetaData = sourceConn.getMetaData();
            
            List<Map<String, Object>> columns = new ArrayList<>();
            ResultSet rs = sourceMetaData.getColumns(null, null, sourceTableName, null);
            while (rs.next())
            {
                Map<String, Object> column = new HashMap<>();
                column.put("columnName", rs.getString("COLUMN_NAME"));
                column.put("dataType", rs.getInt("DATA_TYPE"));
                column.put("typeName", rs.getString("TYPE_NAME"));
                column.put("columnSize", rs.getInt("COLUMN_SIZE"));
                column.put("decimalDigits", rs.getInt("DECIMAL_DIGITS"));
                column.put("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.put("columnDef", rs.getString("COLUMN_DEF"));
                column.put("remarks", rs.getString("REMARKS"));
                columns.add(column);
            }
            rs.close();
            
            if (columns.isEmpty())
            {
                throw new ServiceException("源表不存在或没有字段: " + sourceTableName);
            }

            // 获取源表主键
            List<String> primaryKeys = new ArrayList<>();
            ResultSet pkRs = sourceMetaData.getPrimaryKeys(null, null, sourceTableName);
            while (pkRs.next())
            {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }
            pkRs.close();

            // 构建CREATE TABLE语句
            StringBuilder createTableSql = new StringBuilder();
            createTableSql.append("CREATE TABLE IF NOT EXISTS `").append(targetTableName).append("` (\n");
            
            // 使用列表收集有效字段，避免跳过字段导致的索引问题
            List<Map<String, Object>> validColumns = new ArrayList<>();
            for (Map<String, Object> col : columns)
            {
                String colName = (String) col.get("columnName");
                if (StringUtils.isNotEmpty(colName))
                {
                    validColumns.add(col);
                }
                else
                {
                    log.warn("跳过空字段名");
                }
            }
            
            if (validColumns.isEmpty())
            {
                throw new ServiceException("没有有效的字段可以创建表");
            }
            
            for (int i = 0; i < validColumns.size(); i++)
            {
                Map<String, Object> col = validColumns.get(i);
                String colName = (String) col.get("columnName");
                int dataType = (Integer) col.get("dataType");
                String typeName = (String) col.get("typeName");
                Integer columnSize = (Integer) col.get("columnSize");
                Integer decimalDigits = (Integer) col.get("decimalDigits");
                Boolean nullable = (Boolean) col.get("nullable");
                String columnDef = (String) col.get("columnDef");
                String remarks = (String) col.get("remarks");
                
                // 转义字段名中的反引号（虽然不应该有，但为了安全）
                String escapedColName = colName.replace("`", "``");
                createTableSql.append("  `").append(escapedColName).append("` ");
                
                // 转换数据类型为MySQL类型
                String mysqlType = convertToMySqlType(dataType, typeName, columnSize, decimalDigits);
                if (StringUtils.isEmpty(mysqlType))
                {
                    log.warn("字段 {} 类型转换失败，dataType={}, typeName={}，使用默认类型 VARCHAR(255)", 
                        colName, dataType, typeName);
                    mysqlType = "VARCHAR(255)";
                }
                
                // 记录字段定义信息用于调试
                log.debug("字段定义: name={}, dataType={}, typeName={}, mysqlType={}, nullable={}, columnDef={}", 
                    colName, dataType, typeName, mysqlType, nullable, columnDef);
                
                createTableSql.append(mysqlType);
                
                // 处理 NOT NULL 约束
                if (nullable != null && !nullable)
                {
                    createTableSql.append(" NOT NULL");
                }
                
                // 处理 DEFAULT 值
                if (columnDef != null && !columnDef.trim().isEmpty())
                {
                    // 如果默认值是字符串，需要加引号；如果是数字或函数，直接使用
                    String defValue = columnDef.trim();
                    if (defValue.equalsIgnoreCase("NULL"))
                    {
                        createTableSql.append(" DEFAULT NULL");
                    }
                    else if (defValue.matches("^\\d+(\\.\\d+)?$") || defValue.equalsIgnoreCase("CURRENT_TIMESTAMP"))
                    {
                        // 数字或 CURRENT_TIMESTAMP，直接使用
                        createTableSql.append(" DEFAULT ").append(defValue);
                    }
                    else
                    {
                        // 字符串，需要加引号并转义
                        createTableSql.append(" DEFAULT '").append(defValue.replace("'", "''")).append("'");
                    }
                }
                
                // 处理注释
                if (remarks != null && !remarks.trim().isEmpty())
                {
                    createTableSql.append(" COMMENT '").append(remarks.replace("'", "''")).append("'");
                }
                
                // 添加逗号：如果不是最后一个字段，或者后面还有主键定义，则添加逗号
                if (i < validColumns.size() - 1 || !primaryKeys.isEmpty())
                {
                    createTableSql.append(",");
                }
                createTableSql.append("\n");
            }
            
            // 添加主键
            if (!primaryKeys.isEmpty())
            {
                createTableSql.append("  PRIMARY KEY (");
                for (int i = 0; i < primaryKeys.size(); i++)
                {
                    if (i > 0) createTableSql.append(", ");
                    createTableSql.append("`").append(primaryKeys.get(i)).append("`");
                }
                createTableSql.append(")");
            }
            
            createTableSql.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='从").append(sourceTableName).append("自动创建'");
            
            // 输出生成的SQL语句用于调试
            String finalSql = createTableSql.toString();
            log.debug("生成的CREATE TABLE SQL: \n{}", finalSql);
            
            // 在目标数据库执行CREATE TABLE
            targetConn = localDataSource.getConnection();
            stmt = targetConn.createStatement();
            stmt.executeUpdate(finalSql);
            
            log.info("自动创建目标表成功: sourceTable={}, targetTable={}", sourceTableName, targetTableName);
            return true;
        }
        catch (Exception e)
        {
            log.error("自动创建目标表失败: sourceTable={}, targetTable={}", sourceTableName, targetTableName, e);
            throw new ServiceException("自动创建目标表失败: " + e.getMessage());
        }
        finally
        {
            if (stmt != null)
            {
                try { stmt.close(); } catch (Exception e) { log.warn("关闭Statement失败", e); }
            }
            if (sourceConn != null)
            {
                try { sourceConn.close(); } catch (Exception e) { log.warn("关闭源数据库连接失败", e); }
            }
            if (targetConn != null)
            {
                try { targetConn.close(); } catch (Exception e) { log.warn("关闭目标数据库连接失败", e); }
            }
        }
    }

    /**
     * 转换数据类型为MySQL类型
     */
    private String convertToMySqlType(int dataType, String typeName, Integer columnSize, Integer decimalDigits)
    {
        switch (dataType)
        {
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.NVARCHAR:
            case Types.NCHAR:
                return "VARCHAR(" + (columnSize != null && columnSize > 0 ? columnSize : 255) + ")";
            case Types.LONGVARCHAR:
            case Types.CLOB:
            case Types.NCLOB:
                return "TEXT";
            case Types.INTEGER:
                return "INT";
            case Types.BIGINT:
                return "BIGINT";
            case Types.SMALLINT:
                return "SMALLINT";
            case Types.TINYINT:
                return "TINYINT";
            case Types.DECIMAL:
            case Types.NUMERIC:
                return "DECIMAL(" + (columnSize != null ? columnSize : 10) + "," + (decimalDigits != null ? decimalDigits : 0) + ")";
            case Types.DOUBLE:
            case Types.FLOAT:
                return "DOUBLE";
            case Types.REAL:
                return "FLOAT";
            case Types.DATE:
                return "DATE";
            case Types.TIME:
                return "TIME";
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return "DATETIME";
            case Types.BOOLEAN:
            case Types.BIT:
                return "TINYINT(1)";
            case Types.BLOB:
            case Types.LONGVARBINARY:
                return "BLOB";
            default:
                // 尝试根据类型名称判断
                if (typeName != null)
                {
                    String upperTypeName = typeName.toUpperCase();
                    if (upperTypeName.contains("VARCHAR")) 
                        return "VARCHAR(" + (columnSize != null && columnSize > 0 ? columnSize : 255) + ")";
                    if (upperTypeName.contains("TEXT")) 
                        return "TEXT";
                    if (upperTypeName.contains("INT")) 
                        return "INT";
                    if (upperTypeName.contains("BIGINT")) 
                        return "BIGINT";
                    if (upperTypeName.contains("DECIMAL") || upperTypeName.contains("NUMERIC")) 
                        return "DECIMAL(" + (columnSize != null ? columnSize : 10) + "," + (decimalDigits != null ? decimalDigits : 0) + ")";
                    if (upperTypeName.contains("DOUBLE")) 
                        return "DOUBLE";
                    if (upperTypeName.contains("FLOAT")) 
                        return "FLOAT";
                    if (upperTypeName.contains("DATE")) 
                        return "DATE";
                    if (upperTypeName.contains("TIME")) 
                        return "TIME";
                    if (upperTypeName.contains("DATETIME") || upperTypeName.contains("TIMESTAMP")) 
                        return "DATETIME";
                }
                return "VARCHAR(255)"; // 默认类型
        }
    }

    /**
     * 检查本地数据库中表是否存在
     */
    @Override
    public boolean checkLocalTableExists(String tableName)
    {
        if (localDataSource == null)
        {
            throw new ServiceException("本地数据源未配置");
        }

        if (StringUtils.isEmpty(tableName))
        {
            return false;
        }

        Connection conn = null;
        try
        {
            conn = localDataSource.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            
            // 获取当前数据库名
            String catalog = conn.getCatalog();
            String[] types = {"TABLE", "VIEW"};
            
            ResultSet rs = metaData.getTables(catalog, null, tableName, types);
            boolean exists = rs.next();
            rs.close();
            
            log.debug("检查本地表是否存在: tableName={}, exists={}", tableName, exists);
            return exists;
        }
        catch (Exception e)
        {
            log.error("检查本地表是否存在失败: tableName={}", tableName, e);
            throw new ServiceException("检查本地表是否存在失败: " + e.getMessage());
        }
        finally
        {
            if (conn != null)
            {
                try
                {
                    conn.close();
                }
                catch (Exception e)
                {
                    log.warn("关闭数据库连接失败", e);
                }
            }
        }
    }
    
    /**
     * 获取本地数据库连接（用于查询执行）
     * 
     * @return 数据库连接
     * @throws Exception 连接失败时抛出异常
     */
    @Override
    public Connection getLocalConnection() throws Exception
    {
        if (localDataSource == null)
        {
            throw new ServiceException("本地数据源未配置");
        }
        
        try
        {
            Connection conn = localDataSource.getConnection();
            log.debug("获取本地数据库连接成功");
            return conn;
        }
        catch (Exception e)
        {
            log.error("获取本地数据库连接失败", e);
            ServiceException ex = new ServiceException("获取本地数据库连接失败: " + e.getMessage());
            ex.initCause(e);
            ex.setDetailMessage(e.getMessage());
            throw ex;
        }
    }

    /**
     * 获取本地数据库版本（用于 NL2SQL 提示词，避免生成不兼容语法）
     * MySQL: SELECT VERSION(); 其他库使用 DatabaseMetaData.getDatabaseProductVersion()
     */
    @Override
    public String getLocalDatabaseVersion()
    {
        if (localDataSource == null)
        {
            return null;
        }
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = localDataSource.getConnection();
            String productName = conn.getMetaData().getDatabaseProductName();
            String productVersion = conn.getMetaData().getDatabaseProductVersion();
            if (productName != null && productVersion != null)
            {
                String version = productName + " " + productVersion;
                log.debug("本地数据库版本: {}", version);
                return version;
            }
            if (productName != null && productName.toUpperCase().contains("MYSQL"))
            {
                stmt = conn.createStatement();
                rs = stmt.executeQuery("SELECT VERSION()");
                if (rs.next())
                {
                    return "MySQL " + rs.getString(1);
                }
            }
            return productName != null ? productName : null;
        }
        catch (Exception e)
        {
            log.warn("获取本地数据库版本失败", e);
            return null;
        }
        finally
        {
            if (rs != null) try { rs.close(); } catch (Exception e) { log.trace("关闭ResultSet", e); }
            if (stmt != null) try { stmt.close(); } catch (Exception e) { log.trace("关闭Statement", e); }
            if (conn != null) try { conn.close(); } catch (Exception e) { log.trace("关闭Connection", e); }
        }
    }

    /**
     * 获取本地数据库连接信息（来自 application.yml 的 master 配置）
     * 用于 DataX Writer 写入目标库，密码为 yaml 明文，无需解密
     */
    @Override
    public String[] getLocalDatabaseConnectionInfo()
    {
        if (localDataSource == null)
        {
            return null;
        }
        try
        {
            Class<?> clazz = localDataSource.getClass();
            if ("com.alibaba.druid.pool.DruidDataSource".equals(clazz.getName()))
            {
                java.lang.reflect.Method getUrl = clazz.getMethod("getUrl");
                java.lang.reflect.Method getUsername = clazz.getMethod("getUsername");
                java.lang.reflect.Method getPassword = clazz.getMethod("getPassword");
                String url = (String) getUrl.invoke(localDataSource);
                String username = (String) getUsername.invoke(localDataSource);
                String password = (String) getPassword.invoke(localDataSource);
                if (url != null && username != null && password != null)
                {
                    return new String[]{url, username, password};
                }
            }
        }
        catch (Exception e)
        {
            log.debug("获取 Druid 数据源连接信息失败: {}", e.getMessage());
        }
        return null;
    }
}
