package dev.lhl.query.service.impl;

import dev.lhl.query.domain.AsyncQueryTask;
import dev.lhl.query.domain.QueryRecord;
import dev.lhl.query.mapper.AsyncQueryTaskMapper;
import dev.lhl.query.service.IAsyncQueryService;
import dev.lhl.query.service.IQueryExecutionService;
import dev.lhl.query.service.IQueryRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * 异步查询服务实现
 * 负责管理异步查询任务
 * 
 * @author smart-bi
 */
@Service
public class AsyncQueryServiceImpl implements IAsyncQueryService
{
    private static final Logger log = LoggerFactory.getLogger(AsyncQueryServiceImpl.class);
    
    // 存储正在执行的任务Future，用于取消
    private final ConcurrentHashMap<Long, Future<?>> runningTasks = new ConcurrentHashMap<>();
    
    @Autowired
    private AsyncQueryTaskMapper asyncQueryTaskMapper;
    
    @Autowired
    private IQueryExecutionService queryExecutionService;
    
    @Autowired(required = false)
    private IQueryRecordService queryRecordService;
    
    @Override
    public Long createAsyncTask(QueryRecord queryRecord, Long userId)
    {
        try
        {
            log.info("创建异步查询任务: queryId={}, userId={}", queryRecord.getId(), userId);
            
            AsyncQueryTask task = new AsyncQueryTask();
            task.setQueryId(queryRecord.getId());
            task.setUserId(userId);
            task.setStatus("PENDING");
            task.setProgress(0);
            task.setCreateTime(new Date());
            
            asyncQueryTaskMapper.insertAsyncQueryTask(task);
            
            // 异步执行任务
            executeAsyncTask(task.getId());
            
            return task.getId();
        }
        catch (Exception e)
        {
            log.error("创建异步查询任务失败: queryId={}", queryRecord.getId(), e);
            throw new RuntimeException("创建异步查询任务失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    public void executeAsyncTask(Long taskId)
    {
        final long startTime = System.currentTimeMillis();
        final Long finalTaskId = taskId;
        
        try
        {
            // 1. 获取任务信息
            AsyncQueryTask task = asyncQueryTaskMapper.selectAsyncQueryTaskById(finalTaskId);
            if (task == null)
            {
                log.error("异步查询任务不存在: taskId={}", finalTaskId);
                return;
            }
            
            // 2. 更新任务状态为运行中
            task.setStatus("RUNNING");
            task.setProgress(10);
            task.setStartTime(new Date());
            asyncQueryTaskMapper.updateAsyncQueryTask(task);
            
            // 3. 获取查询记录
            if (queryRecordService == null)
            {
                throw new RuntimeException("查询记录服务未配置");
            }
            
            final QueryRecord queryRecord = queryRecordService.selectQueryRecordById(task.getQueryId());
            if (queryRecord == null)
            {
                throw new RuntimeException("查询记录不存在: queryId=" + task.getQueryId());
            }
            
            final Long userId = task.getUserId();
            
            // 4. 执行查询（异步）
            CompletableFuture<IQueryExecutionService.QueryResult> future = CompletableFuture.supplyAsync(() -> {
                try
                {
                    // 更新进度（重新查询任务）
                    AsyncQueryTask progressTask = asyncQueryTaskMapper.selectAsyncQueryTaskById(finalTaskId);
                    if (progressTask != null)
                    {
                        progressTask.setProgress(30);
                        asyncQueryTaskMapper.updateAsyncQueryTask(progressTask);
                    }
                    
                    // 执行查询
                    IQueryExecutionService.QueryResult result = 
                        queryExecutionService.executeQuery(queryRecord, userId);
                    
                    // 更新进度
                    progressTask = asyncQueryTaskMapper.selectAsyncQueryTaskById(finalTaskId);
                    if (progressTask != null)
                    {
                        progressTask.setProgress(80);
                        asyncQueryTaskMapper.updateAsyncQueryTask(progressTask);
                    }
                    
                    return result;
                }
                catch (Exception e)
                {
                    log.error("执行查询失败: taskId={}", finalTaskId, e);
                    throw new RuntimeException("执行查询失败: " + e.getMessage(), e);
                }
            });
            
            // 存储Future用于取消
            runningTasks.put(finalTaskId, future);
            
            // 5. 等待查询完成
            IQueryExecutionService.QueryResult result = future.get();
            
            // 6. 更新查询记录结果
            if (result.isSuccess())
            {
                String resultJson = com.alibaba.fastjson2.JSON.toJSONString(result.getData());
                queryRecord.setResult(resultJson);
                queryRecord.setDuration(result.getExecutionTime());
                queryRecord.setStatus("SUCCESS");
                queryRecordService.updateQueryRecord(queryRecord);
                
                // 更新任务状态为成功
                AsyncQueryTask finalTask = asyncQueryTaskMapper.selectAsyncQueryTaskById(finalTaskId);
                if (finalTask != null)
                {
                    finalTask.setStatus("SUCCESS");
                    finalTask.setProgress(100);
                    long executionTime = System.currentTimeMillis() - startTime;
                    finalTask.setEndTime(new Date());
                    finalTask.setExecutionTime(executionTime);
                    asyncQueryTaskMapper.updateAsyncQueryTask(finalTask);
                }
            }
            else
            {
                queryRecord.setStatus("FAILED");
                queryRecord.setErrorMessage(result.getErrorMessage());
                queryRecordService.updateQueryRecord(queryRecord);
                
                // 更新任务状态为失败
                AsyncQueryTask finalTask = asyncQueryTaskMapper.selectAsyncQueryTaskById(finalTaskId);
                if (finalTask != null)
                {
                    finalTask.setStatus("FAILED");
                    finalTask.setErrorMessage(result.getErrorMessage());
                    long executionTime = System.currentTimeMillis() - startTime;
                    finalTask.setEndTime(new Date());
                    finalTask.setExecutionTime(executionTime);
                    asyncQueryTaskMapper.updateAsyncQueryTask(finalTask);
                }
            }
            
            // 7. 计算执行时长
            long executionTime = System.currentTimeMillis() - startTime;
            AsyncQueryTask finalTask = asyncQueryTaskMapper.selectAsyncQueryTaskById(finalTaskId);
            if (finalTask != null)
            {
                log.info("异步查询任务完成: taskId={}, status={}, executionTime={}ms", 
                    finalTaskId, finalTask.getStatus(), executionTime);
            }
        }
        catch (Exception e)
        {
            log.error("异步查询任务执行失败: taskId={}", finalTaskId, e);
            
            AsyncQueryTask failedTask = asyncQueryTaskMapper.selectAsyncQueryTaskById(finalTaskId);
            if (failedTask != null)
            {
                failedTask.setStatus("FAILED");
                failedTask.setErrorMessage(e.getMessage());
                failedTask.setEndTime(new Date());
                failedTask.setExecutionTime(System.currentTimeMillis() - startTime);
                asyncQueryTaskMapper.updateAsyncQueryTask(failedTask);
            }
        }
        finally
        {
            // 移除Future
            runningTasks.remove(finalTaskId);
        }
    }
    
    @Override
    public AsyncQueryTask getTaskStatus(Long taskId)
    {
        return asyncQueryTaskMapper.selectAsyncQueryTaskById(taskId);
    }
    
    @Override
    public AsyncQueryTask getTaskStatusByQueryId(Long queryId)
    {
        return asyncQueryTaskMapper.selectAsyncQueryTaskByQueryId(queryId);
    }
    
    @Override
    public boolean cancelTask(Long taskId)
    {
        try
        {
            AsyncQueryTask task = asyncQueryTaskMapper.selectAsyncQueryTaskById(taskId);
            if (task == null)
            {
                return false;
            }
            
            // 如果任务正在运行，尝试取消
            Future<?> future = runningTasks.get(taskId);
            if (future != null && !future.isDone())
            {
                boolean cancelled = future.cancel(true);
                if (cancelled)
                {
                    task.setStatus("CANCELLED");
                    task.setEndTime(new Date());
                    asyncQueryTaskMapper.updateAsyncQueryTask(task);
                    log.info("异步查询任务已取消: taskId={}", taskId);
                    return true;
                }
            }
            
            // 如果任务还未开始，直接更新状态
            if ("PENDING".equals(task.getStatus()))
            {
                task.setStatus("CANCELLED");
                task.setEndTime(new Date());
                asyncQueryTaskMapper.updateAsyncQueryTask(task);
                return true;
            }
            
            return false;
        }
        catch (Exception e)
        {
            log.error("取消异步查询任务失败: taskId={}", taskId, e);
            return false;
        }
    }
}
