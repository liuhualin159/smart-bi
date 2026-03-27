package dev.lhl.dashboard.service.impl;

import dev.lhl.dashboard.domain.DatasourceCardConfig;
import dev.lhl.dashboard.mapper.DatasourceCardConfigMapper;
import dev.lhl.dashboard.service.IDatasourceCardConfigService;
import dev.lhl.datasource.domain.DataSource;
import dev.lhl.datasource.service.IDataSourceService;
import dev.lhl.common.exception.ServiceException;
import dev.lhl.common.utils.DateUtils;
import dev.lhl.common.utils.EncryptUtils;
import dev.lhl.common.utils.SecurityUtils;
import dev.lhl.common.utils.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 数据源卡片配置Service业务层处理
 *
 * @author smart-bi
 */
@Service
public class DatasourceCardConfigServiceImpl implements IDatasourceCardConfigService
{
    private static final Logger log = LoggerFactory.getLogger(DatasourceCardConfigServiceImpl.class);

    private static final Pattern SQL_SELECT_PATTERN = Pattern.compile("(?i)^\\s*SELECT\\s+");
    private static final Pattern SQL_DANGEROUS_PATTERN = Pattern.compile(
            "(?i)\\b(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE)\\b");

    @Autowired
    private DatasourceCardConfigMapper datasourceCardConfigMapper;

    @Autowired
    private IDataSourceService dataSourceService;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public DatasourceCardConfig selectDatasourceCardConfigById(Long id)
    {
        return datasourceCardConfigMapper.selectDatasourceCardConfigById(id);
    }

    @Override
    public DatasourceCardConfig selectByDashboardCardId(Long dashboardCardId)
    {
        return datasourceCardConfigMapper.selectByDashboardCardId(dashboardCardId);
    }

    @Override
    public List<DatasourceCardConfig> selectDatasourceCardConfigList(DatasourceCardConfig config)
    {
        return datasourceCardConfigMapper.selectDatasourceCardConfigList(config);
    }

    @Override
    @Transactional
    public int insertDatasourceCardConfig(DatasourceCardConfig config)
    {
        config.setCreateBy(SecurityUtils.getUsername());
        config.setCreateTime(DateUtils.getNowDate());
        int result = datasourceCardConfigMapper.insertDatasourceCardConfig(config);
        log.info("新增数据源卡片配置成功: id={}, dashboardCardId={}", config.getId(), config.getDashboardCardId());
        return result;
    }

    @Override
    @Transactional
    public int updateDatasourceCardConfig(DatasourceCardConfig config)
    {
        config.setUpdateBy(SecurityUtils.getUsername());
        config.setUpdateTime(DateUtils.getNowDate());
        int result = datasourceCardConfigMapper.updateDatasourceCardConfig(config);
        log.info("修改数据源卡片配置成功: id={}", config.getId());
        return result;
    }

    @Override
    @Transactional
    public int deleteDatasourceCardConfigById(Long id)
    {
        int result = datasourceCardConfigMapper.deleteDatasourceCardConfigById(id);
        log.info("删除数据源卡片配置成功: id={}", id);
        return result;
    }

    @Override
    @Transactional
    public int deleteDatasourceCardConfigByIds(Long[] ids)
    {
        int result = datasourceCardConfigMapper.deleteDatasourceCardConfigByIds(ids);
        log.info("批量删除数据源卡片配置成功: ids={}", Arrays.toString(ids));
        return result;
    }

    @Override
    public Map<String, Object> executeQuery(Long configId)
    {
        DatasourceCardConfig config = datasourceCardConfigMapper.selectDatasourceCardConfigById(configId);
        if (config == null)
        {
            throw new ServiceException("数据源卡片配置不存在: configId=" + configId);
        }
        return doExecuteQuery(config);
    }

    @Override
    public Map<String, Object> previewQuery(DatasourceCardConfig config)
    {
        return doExecuteQuery(config);
    }

    private Map<String, Object> doExecuteQuery(DatasourceCardConfig config)
    {
        String queryType = config.getQueryType();
        if ("SQL".equalsIgnoreCase(queryType))
        {
            return executeSqlQuery(config);
        }
        else if ("API".equalsIgnoreCase(queryType))
        {
            return executeApiQuery(config);
        }
        else
        {
            throw new ServiceException("不支持的查询类型: " + queryType);
        }
    }

    // ========== SQL Query Execution ==========

    private Map<String, Object> executeSqlQuery(DatasourceCardConfig config)
    {
        String sql = config.getSqlTemplate();
        validateSql(sql);

        DataSource ds = dataSourceService.selectDataSourceById(config.getDatasourceId());
        if (ds == null)
        {
            throw new ServiceException("数据源不存在: datasourceId=" + config.getDatasourceId());
        }

        String jdbcUrl = buildJdbcUrl(ds);
        String driverClass = getDriverClass(ds.getSubType());

        String password = ds.getPassword();
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

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            Class.forName(driverClass);
            conn = DriverManager.getConnection(jdbcUrl, ds.getUsername(), password);
            stmt = conn.createStatement();
            stmt.setQueryTimeout(config.getQueryTimeout() != null ? config.getQueryTimeout() : 30);
            stmt.setMaxRows(config.getMaxRows() != null ? config.getMaxRows() : 10000);

            rs = stmt.executeQuery(sql);
            return buildQueryResult(rs);
        }
        catch (ClassNotFoundException e)
        {
            log.error("数据库驱动未找到: subType={}", ds.getSubType(), e);
            throw new ServiceException("数据库驱动未找到: " + ds.getSubType());
        }
        catch (SQLException e)
        {
            log.error("SQL执行失败: sql={}", sql, e);
            throw new ServiceException("SQL执行失败: " + e.getMessage());
        }
        finally
        {
            closeQuietly(rs, stmt, conn);
        }
    }

    private void validateSql(String sql)
    {
        if (StringUtils.isEmpty(sql))
        {
            throw new ServiceException("SQL不能为空");
        }
        if (!SQL_SELECT_PATTERN.matcher(sql).find())
        {
            throw new ServiceException("仅允许SELECT查询语句");
        }
        if (SQL_DANGEROUS_PATTERN.matcher(sql).find())
        {
            throw new ServiceException("SQL包含禁止的操作关键字");
        }
    }

    private Map<String, Object> buildQueryResult(ResultSet rs) throws SQLException
    {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        List<Map<String, Object>> columns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++)
        {
            Map<String, Object> col = new LinkedHashMap<>();
            col.put("name", meta.getColumnLabel(i));
            col.put("type", meta.getColumnTypeName(i));
            columns.add(col);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next())
        {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++)
            {
                row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            rows.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("total", rows.size());
        return result;
    }

    private String buildJdbcUrl(DataSource dataSource)
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

    private void closeQuietly(ResultSet rs, Statement stmt, Connection conn)
    {
        if (rs != null) { try { rs.close(); } catch (SQLException ignored) {} }
        if (stmt != null) { try { stmt.close(); } catch (SQLException ignored) {} }
        if (conn != null) { try { conn.close(); } catch (SQLException ignored) {} }
    }

    // ========== API Query Execution ==========

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeApiQuery(DatasourceCardConfig config)
    {
        if (restTemplate == null)
        {
            throw new ServiceException("RestTemplate未配置，无法执行API查询");
        }

        String apiUrl = config.getApiUrl();
        String apiMethod = config.getApiMethod();
        if (StringUtils.isEmpty(apiUrl))
        {
            throw new ServiceException("API地址不能为空");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.isNotEmpty(config.getApiHeaders()))
        {
            try
            {
                Map<String, String> customHeaders = objectMapper.readValue(
                        config.getApiHeaders(), new TypeReference<Map<String, String>>() {});
                customHeaders.forEach(headers::set);
            }
            catch (Exception e)
            {
                log.warn("解析自定义Header失败", e);
            }
        }

        HttpEntity<String> entity = new HttpEntity<>(config.getApiBody(), headers);
        HttpMethod method = HttpMethod.valueOf(
                StringUtils.isNotEmpty(apiMethod) ? apiMethod.toUpperCase() : "GET");

        ResponseEntity<String> response;
        try
        {
            response = restTemplate.exchange(apiUrl, method, entity, String.class);
        }
        catch (Exception e)
        {
            log.error("API调用失败: url={}", apiUrl, e);
            throw new ServiceException("API调用失败: " + e.getMessage());
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null)
        {
            throw new ServiceException("API返回异常: status=" + response.getStatusCode());
        }

        try
        {
            Object parsed = objectMapper.readValue(response.getBody(), Object.class);
            Object data = extractDataByPath(parsed, config.getResponseDataPath());

            if (data instanceof List)
            {
                return buildApiResult((List<Map<String, Object>>) data);
            }
            else if (data instanceof Map)
            {
                List<Map<String, Object>> singleRow = new ArrayList<>();
                singleRow.add((Map<String, Object>) data);
                return buildApiResult(singleRow);
            }
            else
            {
                throw new ServiceException("API响应数据格式不支持，期望数组或对象");
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("API响应解析失败", e);
            throw new ServiceException("API响应解析失败: " + e.getMessage());
        }
    }

    /**
     * 按路径提取嵌套数据，支持 "data.records" 格式
     */
    @SuppressWarnings("unchecked")
    private Object extractDataByPath(Object root, String path)
    {
        if (StringUtils.isEmpty(path))
        {
            return root;
        }
        Object current = root;
        for (String key : path.split("\\."))
        {
            if (current instanceof Map)
            {
                current = ((Map<String, Object>) current).get(key);
            }
            else
            {
                throw new ServiceException("响应数据路径无效: " + path);
            }
            if (current == null)
            {
                throw new ServiceException("响应数据路径不存在: " + path);
            }
        }
        return current;
    }

    private Map<String, Object> buildApiResult(List<Map<String, Object>> rows)
    {
        List<Map<String, Object>> columns = new ArrayList<>();
        if (!rows.isEmpty())
        {
            for (String key : rows.get(0).keySet())
            {
                Map<String, Object> col = new LinkedHashMap<>();
                col.put("name", key);
                col.put("type", "STRING");
                columns.add(col);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("total", rows.size());
        return result;
    }
}
