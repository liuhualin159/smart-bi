package dev.lhl.query.service;

import dev.lhl.query.domain.AsyncQueryTask;
import dev.lhl.query.domain.QueryRecord;

/**
 * 异步查询服务接口
 * 负责管理异步查询任务
 * 
 * @author smart-bi
 */
public interface IAsyncQueryService
{
    /**
     * 创建异步查询任务
     * 
     * @param queryRecord 查询记录
     * @param userId 用户ID
     * @return 任务ID
     */
    Long createAsyncTask(QueryRecord queryRecord, Long userId);
    
    /**
     * 执行异步查询任务
     * 
     * @param taskId 任务ID
     */
    void executeAsyncTask(Long taskId);
    
    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @return 任务对象
     */
    AsyncQueryTask getTaskStatus(Long taskId);
    
    /**
     * 获取任务状态（通过查询记录ID）
     * 
     * @param queryId 查询记录ID
     * @return 任务对象
     */
    AsyncQueryTask getTaskStatusByQueryId(Long queryId);
    
    /**
     * 取消任务
     * 
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean cancelTask(Long taskId);
}
