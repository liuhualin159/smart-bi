package dev.lhl.query.mapper;

import dev.lhl.query.domain.AsyncQueryTask;
import java.util.List;

/**
 * 异步查询任务Mapper接口
 * 
 * @author smart-bi
 */
public interface AsyncQueryTaskMapper
{
    AsyncQueryTask selectAsyncQueryTaskById(Long id);
    AsyncQueryTask selectAsyncQueryTaskByQueryId(Long queryId);
    List<AsyncQueryTask> selectAsyncQueryTaskList(AsyncQueryTask asyncQueryTask);
    int insertAsyncQueryTask(AsyncQueryTask asyncQueryTask);
    int updateAsyncQueryTask(AsyncQueryTask asyncQueryTask);
    int deleteAsyncQueryTaskById(Long id);
    int deleteAsyncQueryTaskByIds(Long[] ids);
}
