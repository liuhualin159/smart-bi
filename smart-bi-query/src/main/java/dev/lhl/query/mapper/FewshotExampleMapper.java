package dev.lhl.query.mapper;

import dev.lhl.query.domain.FewshotExample;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Few-shot 示例 Mapper 接口
 */
public interface FewshotExampleMapper
{
    FewshotExample selectById(Long id);

    List<FewshotExample> selectList(FewshotExample query);

    /**
     * 查询启用的示例，按数据源过滤（包含通用示例 datasource_id IS NULL）
     */
    List<FewshotExample> selectEnabledByDatasource(@Param("datasourceId") Long datasourceId, @Param("limit") int limit);

    int insert(FewshotExample example);

    int update(FewshotExample example);

    int deleteById(Long id);

    int updateEnabled(@Param("id") Long id, @Param("enabled") Integer enabled);

    int batchUpdateEnabled(@Param("ids") List<Long> ids, @Param("enabled") Integer enabled);
}
