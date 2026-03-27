package dev.lhl.datasource.mapper;

import dev.lhl.datasource.domain.DataSource;
import java.util.List;

/**
 * 数据源Mapper接口
 * 
 * @author smart-bi
 */
public interface DataSourceMapper
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
     * 删除数据源
     * 
     * @param id 数据源ID
     * @return 结果
     */
    public int deleteDataSourceById(Long id);

    /**
     * 批量删除数据源
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteDataSourceByIds(Long[] ids);
}
