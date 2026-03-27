package dev.lhl.query.service.impl;

import java.util.List;
import dev.lhl.query.domain.QueryRecord;
import dev.lhl.query.mapper.QueryRecordMapper;
import dev.lhl.query.service.IQueryRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 查询记录Service业务层处理
 * 
 * @author smart-bi
 */
@Service
public class QueryRecordServiceImpl implements IQueryRecordService
{
    private static final Logger log = LoggerFactory.getLogger(QueryRecordServiceImpl.class);

    @Autowired
    private QueryRecordMapper queryRecordMapper;

    /**
     * 查询查询记录列表
     * 
     * @param queryRecord 查询记录
     * @return 查询记录
     */
    @Override
    public List<QueryRecord> selectQueryRecordList(QueryRecord queryRecord)
    {
        return queryRecordMapper.selectQueryRecordList(queryRecord);
    }

    /**
     * 根据ID查询查询记录
     * 
     * @param id 查询记录ID
     * @return 查询记录
     */
    @Override
    public QueryRecord selectQueryRecordById(Long id)
    {
        return queryRecordMapper.selectQueryRecordById(id);
    }

    /**
     * 新增查询记录
     * 
     * @param queryRecord 查询记录
     * @return 结果
     */
    @Override
    public int insertQueryRecord(QueryRecord queryRecord)
    {
        return queryRecordMapper.insertQueryRecord(queryRecord);
    }

    /**
     * 修改查询记录
     * 
     * @param queryRecord 查询记录
     * @return 结果
     */
    @Override
    public int updateQueryRecord(QueryRecord queryRecord)
    {
        return queryRecordMapper.updateQueryRecord(queryRecord);
    }

    /**
     * 批量删除查询记录
     * 
     * @param ids 需要删除的查询记录ID
     * @return 结果
     */
    @Override
    public int deleteQueryRecordByIds(Long[] ids)
    {
        return queryRecordMapper.deleteQueryRecordByIds(ids);
    }
}
