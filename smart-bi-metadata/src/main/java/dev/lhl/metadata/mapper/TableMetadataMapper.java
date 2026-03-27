package dev.lhl.metadata.mapper;

import dev.lhl.metadata.domain.TableMetadata;

import java.util.List;

import org.apache.ibatis.annotations.Param;

/**
 * 表元数据Mapper接口
 * 
 * @author smart-bi
 */
public interface TableMetadataMapper
{
    public TableMetadata selectTableMetadataById(Long id);
    public List<TableMetadata> selectTableMetadataList(TableMetadata tableMetadata);
    public int insertTableMetadata(TableMetadata tableMetadata);
    public int updateTableMetadata(TableMetadata tableMetadata);
    /** 乐观锁更新（仅更新用途/可见性/粒度），返回影响行数，0 表示冲突 */
    public int updateTableMetadataWithOptimisticLock(TableMetadata tableMetadata);
    public int deleteTableMetadataById(Long id);
    public int deleteTableMetadataByIds(Long[] ids);
    /** 批量更新用途与可见性 */
    int batchUpdateTableMetadata(@Param("ids") Long[] ids, @Param("tableUsage") String tableUsage, @Param("nl2sqlVisibilityLevel") String nl2sqlVisibilityLevel);

    /** NL2SQL 白名单：仅 NORMAL、PREFERRED；restrictToTableIds 为空则全部，否则取交集 */
    List<TableMetadata> selectTableMetadataListForNl2Sql(@Param("tableIds") List<Long> tableIds);

    /** 按表名精确查询 */
    TableMetadata selectByTableName(@Param("tableName") String tableName);

    /** 自动补全：按表名/注释模糊搜索，仅 NORMAL/PREFERRED，限制条数 */
    List<TableMetadata> selectByKeywordForAutocomplete(@Param("keyword") String keyword, @Param("limit") int limit);
}
