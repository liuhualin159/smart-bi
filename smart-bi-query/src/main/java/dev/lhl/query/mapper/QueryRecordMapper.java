package dev.lhl.query.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import dev.lhl.query.domain.QueryRecord;

/**
 * 查询记录Mapper接口
 * 
 * @author smart-bi
 */
public interface QueryRecordMapper
{
    /**
     * 查询查询记录列表
     * 
     * @param queryRecord 查询记录
     * @return 查询记录集合
     */
    public List<QueryRecord> selectQueryRecordList(QueryRecord queryRecord);

    /**
     * 根据ID查询查询记录
     * 
     * @param id 查询记录ID
     * @return 查询记录
     */
    public QueryRecord selectQueryRecordById(Long id);

    /**
     * 新增查询记录
     * 
     * @param queryRecord 查询记录
     * @return 结果
     */
    public int insertQueryRecord(QueryRecord queryRecord);

    /**
     * 修改查询记录
     * 
     * @param queryRecord 查询记录
     * @return 结果
     */
    public int updateQueryRecord(QueryRecord queryRecord);

    /**
     * 删除查询记录
     * 
     * @param id 查询记录ID
     * @return 结果
     */
    public int deleteQueryRecordById(Long id);

    /**
     * 批量删除查询记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteQueryRecordByIds(Long[] ids);

    /**
     * 获取最近的成功查询记录（用于缓存预热）
     *
     * @param limit 条数限制
     * @return 查询记录列表
     */
    List<QueryRecord> selectRecentSuccessRecords(@Param("limit") int limit);

    /**
     * 获取慢查询记录（execution_time >= thresholdMs）
     */
    List<QueryRecord> selectSlowQueryRecords(@Param("thresholdMs") long thresholdMs, @Param("limit") int limit);
}
