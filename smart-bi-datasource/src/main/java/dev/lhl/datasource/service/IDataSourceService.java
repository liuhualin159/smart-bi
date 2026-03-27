package dev.lhl.datasource.service;

import java.util.List;
import dev.lhl.datasource.domain.DataSource;

/**
 * 数据源Service接口
 * 
 * @author smart-bi
 */
public interface IDataSourceService
{
    /**
     * 查询数据源
     * 
     * @param id 数据源ID
     * @return 数据源
     */
    public DataSource selectDataSourceById(Long id);

    /**
     * 查询数据源列表
     * 
     * @param dataSource 数据源
     * @return 数据源集合
     */
    public List<DataSource> selectDataSourceList(DataSource dataSource);

    /**
     * 新增数据源
     * 
     * @param dataSource 数据源
     * @return 结果
     */
    public int insertDataSource(DataSource dataSource);

    /**
     * 修改数据源
     * 
     * @param dataSource 数据源
     * @return 结果
     */
    public int updateDataSource(DataSource dataSource);

    /**
     * 批量删除数据源
     * 
     * @param ids 需要删除的数据源ID
     * @return 结果
     */
    public int deleteDataSourceByIds(Long[] ids);

    /**
     * 删除数据源信息
     * 
     * @param id 数据源ID
     * @return 结果
     */
    public int deleteDataSourceById(Long id);

    /**
     * 测试数据源连接
     * 
     * @param dataSource 数据源
     * @return true表示连接成功
     */
    public boolean testConnection(DataSource dataSource);

    /**
     * 查询数据源的表列表
     * 
     * @param dataSourceId 数据源ID
     * @return 表列表（包含表名和注释）
     */
    public List<java.util.Map<String, Object>> getTableList(Long dataSourceId);

    /**
     * 查询指定表的字段列表
     * 
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @return 字段列表（包含字段名、类型、注释等）
     */
    public List<java.util.Map<String, Object>> getColumnList(Long dataSourceId, String tableName);

    /**
     * 查询本地数据库的表列表（用于ETL目标表选择、元数据管理）
     * 
     * @return 表列表（包含表名和注释）
     */
    public List<java.util.Map<String, Object>> getLocalTableList();

    /**
     * 查询本地数据库指定表的字段列表（用于元数据字段管理）
     * 
     * @param tableName 表名
     * @return 字段列表（包含字段名、类型、注释等）
     */
    public List<java.util.Map<String, Object>> getLocalColumnList(String tableName);

    /**
     * 根据源表结构自动创建目标表
     * 
     * @param sourceDataSourceId 源数据源ID
     * @param sourceTableName 源表名
     * @param targetTableName 目标表名
     * @return true表示创建成功
     */
    public boolean createTargetTableFromSource(Long sourceDataSourceId, String sourceTableName, String targetTableName);

    /**
     * 检查本地数据库中表是否存在
     * 
     * @param tableName 表名
     * @return true表示表存在
     */
    public boolean checkLocalTableExists(String tableName);
    
    /**
     * 获取本地数据库连接（用于查询执行）
     * 
     * @return 数据库连接
     * @throws Exception 连接失败时抛出异常
     */
    public java.sql.Connection getLocalConnection() throws Exception;

    /**
     * 获取本地数据库版本（用于 NL2SQL 提示词，避免生成不兼容语法）
     * 
     * @return 版本字符串（如 "8.0.32"），获取失败时返回 null
     */
    public String getLocalDatabaseVersion();

    /**
     * 获取本地数据库连接信息（来自 application.yml，用于 DataX Writer 写入目标库）
     * 密码为 yaml 中的明文，无需解密
     * 
     * @return [0]=jdbcUrl, [1]=username, [2]=password；若未配置则返回 null
     */
    public String[] getLocalDatabaseConnectionInfo();
}
