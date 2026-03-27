package dev.lhl.query.service;

import java.util.List;
import dev.lhl.query.domain.QueryRecord;

/**
 * 查询记录Service接口
 * 
 * @author smart-bi
 */
public interface IQueryRecordService
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
     * 批量删除查询记录
     * 
     * @param ids 需要删除的查询记录ID
     * @return 结果
     */
    public int deleteQueryRecordByIds(Long[] ids);
}
