package dev.lhl.etl.service.impl;

import dev.lhl.datasource.domain.DataSource;
import dev.lhl.datasource.service.IDataSourceService;
import dev.lhl.etl.domain.EtlTask;
import dev.lhl.etl.domain.EtlTaskExecution;
import dev.lhl.etl.mapper.EtlTaskExecutionMapper;
import dev.lhl.etl.mapper.EtlTaskMapper;
import dev.lhl.etl.service.DataXExecutionService;
import dev.lhl.etl.service.IEtlTaskService;
import dev.lhl.common.utils.DateUtils;
import dev.lhl.common.utils.SecurityUtils;
import dev.lhl.common.utils.StringUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ETL任务Service业务层处理
 * 
 * @author smart-bi
 */
@Service
public class EtlTaskServiceImpl implements IEtlTaskService
{
    private static final Logger log = LoggerFactory.getLogger(EtlTaskServiceImpl.class);

    @Autowired
    private EtlTaskMapper etlTaskMapper;

    @Autowired
    private EtlTaskExecutionMapper etlTaskExecutionMapper;

    @Autowired
    private DataXExecutionService dataXExecutionService;

    @Autowired
    private IDataSourceService dataSourceService;

    @Autowired(required = false)
    private dev.lhl.quartz.service.ISysJobService sysJobService;

    /**
     * 查询ETL任务
     * 
     * @param id 任务ID
     * @return ETL任务
     */
    @Override
    public EtlTask selectEtlTaskById(Long id)
    {
        return etlTaskMapper.selectEtlTaskById(id);
    }

    /**
     * 查询ETL任务列表
     * 
     * @param etlTask ETL任务
     * @return ETL任务集合
     */
    @Override
    public List<EtlTask> selectEtlTaskList(EtlTask etlTask)
    {
        return etlTaskMapper.selectEtlTaskList(etlTask);
    }

    /**
     * 新增ETL任务
     * 
     * @param etlTask ETL任务
     * @return 结果
     */
    @Override
    @Transactional
    public int insertEtlTask(EtlTask etlTask)
    {
        try
        {
            // 设置默认值
            if (etlTask.getRetryCount() == null)
            {
                etlTask.setRetryCount(3);
            }
            if (StringUtils.isEmpty(etlTask.getRetryInterval()))
            {
                etlTask.setRetryInterval("[1,5,15]"); // 默认重试间隔：1分钟、5分钟、15分钟
            }
            if (StringUtils.isEmpty(etlTask.getStatus()))
            {
                etlTask.setStatus("ACTIVE");
            }

            etlTask.setCreateBy(SecurityUtils.getUsername());
            etlTask.setCreateTime(DateUtils.getNowDate());
            
            int result = etlTaskMapper.insertEtlTask(etlTask);
            
            // 如果调度类型为CRON，创建Quartz任务
            if ("CRON".equals(etlTask.getScheduleType()) && StringUtils.isNotEmpty(etlTask.getCronExpression()))
            {
                createQuartzJob(etlTask);
            }
            
            log.info("新增ETL任务成功: taskId={}, taskName={}", etlTask.getId(), etlTask.getName());
            return result;
        }
        catch (Exception e)
        {
            log.error("新增ETL任务失败", e);
            throw new RuntimeException("新增ETL任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 修改ETL任务
     * 
     * @param etlTask ETL任务
     * @return 结果
     */
    @Override
    @Transactional
    public int updateEtlTask(EtlTask etlTask)
    {
        try
        {
            etlTask.setUpdateBy(SecurityUtils.getUsername());
            etlTask.setUpdateTime(DateUtils.getNowDate());
            
            int result = etlTaskMapper.updateEtlTask(etlTask);
            
            // 更新Quartz任务
            if ("CRON".equals(etlTask.getScheduleType()) && StringUtils.isNotEmpty(etlTask.getCronExpression()))
            {
                updateQuartzJob(etlTask);
            }
            else
            {
                // 如果改为非CRON调度，删除Quartz任务
                deleteQuartzJob(etlTask.getId());
            }
            
            log.info("修改ETL任务成功: taskId={}", etlTask.getId());
            return result;
        }
        catch (Exception e)
        {
            log.error("修改ETL任务失败: taskId={}", etlTask.getId(), e);
            throw new RuntimeException("修改ETL任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量删除ETL任务
     * 
     * @param ids 需要删除的任务ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteEtlTaskByIds(Long[] ids)
    {
        try
        {
            // 删除Quartz任务
            for (Long id : ids)
            {
                deleteQuartzJob(id);
            }
            
            int result = etlTaskMapper.deleteEtlTaskByIds(ids);
            log.info("批量删除ETL任务成功: ids={}", ids);
            return result;
        }
        catch (Exception e)
        {
            log.error("批量删除ETL任务失败: ids={}", ids, e);
            throw new RuntimeException("批量删除ETL任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除ETL任务信息
     * 
     * @param id 任务ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteEtlTaskById(Long id)
    {
        try
        {
            // 删除Quartz任务
            deleteQuartzJob(id);
            
            int result = etlTaskMapper.deleteEtlTaskById(id);
            log.info("删除ETL任务成功: taskId={}", id);
            return result;
        }
        catch (Exception e)
        {
            log.error("删除ETL任务失败: taskId={}", id, e);
            throw new RuntimeException("删除ETL任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 手动触发ETL任务执行
     * 
     * @param id 任务ID
     * @return 执行记录ID
     */
    @Override
    public Long triggerEtlTask(Long id)
    {
        try
        {
            EtlTask etlTask = etlTaskMapper.selectEtlTaskById(id);
            if (etlTask == null)
            {
                throw new RuntimeException("ETL任务不存在: taskId=" + id);
            }
            
            if (!"ACTIVE".equals(etlTask.getStatus()))
            {
                throw new RuntimeException("ETL任务未启用，无法执行: taskId=" + id);
            }
            
            log.info("手动触发ETL任务: taskId={}, taskName={}", id, etlTask.getName());
            EtlTaskExecution execution = executeEtlTask(id);
            return execution.getId();
        }
        catch (Exception e)
        {
            log.error("手动触发ETL任务失败: taskId={}", id, e);
            throw new RuntimeException("手动触发ETL任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 暂停ETL任务
     * 
     * @param id 任务ID
     * @return 结果
     */
    @Override
    @Transactional
    public int pauseEtlTask(Long id)
    {
        try
        {
            EtlTask etlTask = etlTaskMapper.selectEtlTaskById(id);
            if (etlTask == null)
            {
                throw new RuntimeException("ETL任务不存在: taskId=" + id);
            }
            
            if (!"ACTIVE".equals(etlTask.getStatus()))
            {
                throw new RuntimeException("ETL任务状态不正确，无法暂停: taskId=" + id + ", status=" + etlTask.getStatus());
            }
            
            etlTask.setStatus("PAUSED");
            etlTask.setUpdateBy(SecurityUtils.getUsername());
            etlTask.setUpdateTime(DateUtils.getNowDate());
            
            // 暂停Quartz任务
            pauseQuartzJob(id);
            
            int result = etlTaskMapper.updateEtlTask(etlTask);
            log.info("暂停ETL任务成功: taskId={}", id);
            return result;
        }
        catch (Exception e)
        {
            log.error("暂停ETL任务失败: taskId={}", id, e);
            throw new RuntimeException("暂停ETL任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 恢复ETL任务
     * 
     * @param id 任务ID
     * @return 结果
     */
    @Override
    @Transactional
    public int resumeEtlTask(Long id)
    {
        try
        {
            EtlTask etlTask = etlTaskMapper.selectEtlTaskById(id);
            if (etlTask == null)
            {
                throw new RuntimeException("ETL任务不存在: taskId=" + id);
            }
            
            if (!"PAUSED".equals(etlTask.getStatus()))
            {
                throw new RuntimeException("ETL任务状态不正确，无法恢复: taskId=" + id + ", status=" + etlTask.getStatus());
            }
            
            etlTask.setStatus("ACTIVE");
            etlTask.setUpdateBy(SecurityUtils.getUsername());
            etlTask.setUpdateTime(DateUtils.getNowDate());
            
            // 恢复Quartz任务
            resumeQuartzJob(id);
            
            int result = etlTaskMapper.updateEtlTask(etlTask);
            log.info("恢复ETL任务成功: taskId={}", id);
            return result;
        }
        catch (Exception e)
        {
            log.error("恢复ETL任务失败: taskId={}", id, e);
            throw new RuntimeException("恢复ETL任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行ETL任务
     * 包含完整的错误处理、重试逻辑、断点续跑逻辑
     * 
     * @param taskId 任务ID
     * @return 执行记录
     */
    @Override
    @Transactional
    public EtlTaskExecution executeEtlTask(Long taskId)
    {
        EtlTaskExecution execution = null;
        try
        {
            // 1. 获取ETL任务配置
            EtlTask etlTask = etlTaskMapper.selectEtlTaskById(taskId);
            if (etlTask == null)
            {
                throw new RuntimeException("ETL任务不存在: taskId=" + taskId);
            }
            
            // 2. 获取数据源
            DataSource dataSource = dataSourceService.selectDataSourceById(etlTask.getDatasourceId());
            if (dataSource == null)
            {
                throw new RuntimeException("数据源不存在: datasourceId=" + etlTask.getDatasourceId());
            }
            
            // 3. 创建执行记录
            execution = createExecution(etlTask);
            etlTaskExecutionMapper.insertEtlTaskExecution(execution);
            
            log.info("开始执行ETL任务: taskId={}, executionId={}", taskId, execution.getId());
            
            // 4. 获取断点信息（增量抽取时）
            String checkpoint = null;
            if ("INCREMENTAL".equals(etlTask.getExtractMode()))
            {
                checkpoint = getLastCheckpoint(taskId);
            }
            
            // 5. 执行ETL任务（带重试逻辑）
            executeWithRetry(etlTask, dataSource, execution, checkpoint);
            
            // 6. 更新执行记录为成功
            execution.setStatus("SUCCESS");
            execution.setEndTime(new Date());
            if (execution.getStartTime() != null)
            {
                execution.setDuration(System.currentTimeMillis() - execution.getStartTime().getTime());
            }
            etlTaskExecutionMapper.updateEtlTaskExecution(execution);
            
            // 7. 更新任务最后运行时间
            etlTask.setLastRunTime(new Date());
            etlTaskMapper.updateEtlTask(etlTask);
            
            log.info("ETL任务执行成功: taskId={}, executionId={}, duration={}ms", 
                taskId, execution.getId(), execution.getDuration());
            
            return execution;
        }
        catch (Exception e)
        {
            log.error("ETL任务执行失败: taskId={}", taskId, e);
            
            // 更新执行记录为失败
            if (execution != null)
            {
                execution.setStatus("FAILED");
                execution.setEndTime(new Date());
                execution.setErrorMessage(e.getMessage());
                if (execution.getStartTime() != null)
                {
                    execution.setDuration(System.currentTimeMillis() - execution.getStartTime().getTime());
                }
                etlTaskExecutionMapper.updateEtlTaskExecution(execution);
            }
            
            throw new RuntimeException("ETL任务执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建执行记录
     */
    private EtlTaskExecution createExecution(EtlTask etlTask)
    {
        EtlTaskExecution execution = new EtlTaskExecution();
        execution.setTaskId(etlTask.getId());
        execution.setStatus("RUNNING");
        execution.setStartTime(new Date());
        execution.setCreateTime(new Date());
        return execution;
    }

    /**
     * 带重试逻辑执行ETL任务
     */
    private void executeWithRetry(EtlTask etlTask, DataSource dataSource, 
                                  EtlTaskExecution execution, String checkpoint)
    {
        int retryCount = etlTask.getRetryCount() != null ? etlTask.getRetryCount() : 3;
        JSONArray retryIntervals = JSON.parseArray(etlTask.getRetryInterval());
        
        Exception lastException = null;
        for (int attempt = 0; attempt <= retryCount; attempt++)
        {
            try
            {
                if (attempt > 0)
                {
                    // 等待重试间隔
                    int intervalMinutes = retryIntervals != null && retryIntervals.size() > attempt - 1 
                        ? retryIntervals.getIntValue(attempt - 1) : 1;
                    log.info("等待{}分钟后重试: taskId={}, attempt={}", intervalMinutes, etlTask.getId(), attempt);
                    try
                    {
                        Thread.sleep(TimeUnit.MINUTES.toMillis(intervalMinutes));
                    }
                    catch (InterruptedException ie)
                    {
                        // 恢复中断状态
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("ETL任务重试等待被中断: taskId=" + etlTask.getId(), ie);
                    }
                }
                
                // 执行ETL任务
                executeEtlTaskInternal(etlTask, dataSource, execution, checkpoint);
                
                // 执行成功，退出重试循环
                return;
            }
            catch (Exception e)
            {
                lastException = e;
                log.warn("ETL任务执行失败，准备重试: taskId={}, attempt={}/{}, error={}", 
                    etlTask.getId(), attempt, retryCount, e.getMessage());
                
                // 判断是否可重试的错误
                if (!isRetryableError(e))
                {
                    log.error("ETL任务执行失败，错误不可重试: taskId={}, error={}", etlTask.getId(), e.getMessage());
                    throw e;
                }
            }
        }
        
        // 所有重试都失败
        throw new RuntimeException("ETL任务执行失败，已重试" + retryCount + "次: " + 
            (lastException != null ? lastException.getMessage() : "未知错误"), lastException);
    }

    /**
     * 执行ETL任务内部逻辑
     */
    private void executeEtlTaskInternal(EtlTask etlTask, DataSource dataSource, 
                                       EtlTaskExecution execution, String checkpoint)
    {
        try
        {
            // 1. 验证数据源连接
            if (!dataSourceService.testConnection(dataSource))
            {
                throw new RuntimeException("数据源连接失败: datasourceId=" + dataSource.getId());
            }
            
            // 2. 调用DataX执行服务
            Long executionId = dataXExecutionService.execute(etlTask);
            
            // 3. 保存断点信息（增量抽取时）
            if ("INCREMENTAL".equals(etlTask.getExtractMode()) && checkpoint != null)
            {
                execution.setCheckpoint(checkpoint);
                etlTaskExecutionMapper.updateEtlTaskExecution(execution);
            }
            
            log.debug("ETL任务执行完成: taskId={}, executionId={}", etlTask.getId(), executionId);
        }
        catch (Exception e)
        {
            log.error("ETL任务执行内部逻辑失败: taskId={}", etlTask.getId(), e);
            throw e;
        }
    }

    /**
     * 判断错误是否可重试
     */
    private boolean isRetryableError(Exception e)
    {
        String errorMessage = e.getMessage();
        if (errorMessage == null)
        {
            return true; // 未知错误，允许重试
        }
        
        // 数据源连接失败、网络错误等可重试
        if (errorMessage.contains("连接失败") || 
            errorMessage.contains("Connection") ||
            errorMessage.contains("网络") ||
            errorMessage.contains("timeout"))
        {
            return true;
        }
        
        // 配置错误、SQL错误等不可重试
        if (errorMessage.contains("配置错误") ||
            errorMessage.contains("SQL") ||
            errorMessage.contains("语法"))
        {
            return false;
        }
        
        return true; // 默认允许重试
    }

    /**
     * 获取上次执行的断点信息
     */
    private String getLastCheckpoint(Long taskId)
    {
        try
        {
            List<EtlTaskExecution> executions = etlTaskExecutionMapper.selectEtlTaskExecutionListByTaskId(taskId);
            for (EtlTaskExecution execution : executions)
            {
                if ("SUCCESS".equals(execution.getStatus()) && 
                    StringUtils.isNotEmpty(execution.getCheckpoint()))
                {
                    return execution.getCheckpoint();
                }
            }
        }
        catch (Exception e)
        {
            log.warn("获取断点信息失败: taskId={}", taskId, e);
        }
        return null;
    }

    /**
     * 查询ETL任务执行记录列表
     * 
     * @param taskId 任务ID
     * @return 执行记录集合
     */
    @Override
    public List<EtlTaskExecution> selectEtlTaskExecutionListByTaskId(Long taskId)
    {
        return etlTaskExecutionMapper.selectEtlTaskExecutionListByTaskId(taskId);
    }

    /**
     * 创建Quartz任务
     */
    private void createQuartzJob(EtlTask etlTask)
    {
        if (sysJobService == null)
        {
            log.warn("Quartz服务未配置，跳过创建Quartz任务: taskId={}", etlTask.getId());
            return;
        }

        try
        {
            dev.lhl.quartz.domain.SysJob sysJob = new dev.lhl.quartz.domain.SysJob();
            sysJob.setJobId(etlTask.getId());
            sysJob.setJobName(etlTask.getName());
            sysJob.setJobGroup("ETL_TASK");
            // invokeTarget格式：beanName.methodName(params)
            sysJob.setInvokeTarget("etlTaskService.executeEtlTask(" + etlTask.getId() + "L)");
            sysJob.setCronExpression(etlTask.getCronExpression());
            sysJob.setMisfirePolicy("3"); // 不触发立即执行
            sysJob.setConcurrent("1"); // 禁止并发执行
            sysJob.setStatus("ACTIVE".equals(etlTask.getStatus()) ? "0" : "1"); // 0=正常, 1=暂停
            sysJob.setCreateBy(etlTask.getCreateBy());
            sysJob.setCreateTime(etlTask.getCreateTime());
            sysJob.setRemark("ETL任务: " + etlTask.getName());

            sysJobService.insertJob(sysJob);
            log.info("创建Quartz任务成功: taskId={}, jobName={}", etlTask.getId(), etlTask.getName());
        }
        catch (Exception e)
        {
            log.error("创建Quartz任务失败: taskId={}", etlTask.getId(), e);
            // 不抛出异常，允许ETL任务创建成功，但Quartz任务创建失败
        }
    }

    /**
     * 更新Quartz任务
     */
    private void updateQuartzJob(EtlTask etlTask)
    {
        if (sysJobService == null)
        {
            log.warn("Quartz服务未配置，跳过更新Quartz任务: taskId={}", etlTask.getId());
            return;
        }

        try
        {
            dev.lhl.quartz.domain.SysJob sysJob = sysJobService.selectJobById(etlTask.getId());
            if (sysJob == null)
            {
                // 如果Quartz任务不存在，创建新的
                createQuartzJob(etlTask);
                return;
            }

            sysJob.setJobName(etlTask.getName());
            sysJob.setInvokeTarget("etlTaskService.executeEtlTask(" + etlTask.getId() + "L)");
            sysJob.setCronExpression(etlTask.getCronExpression());
            sysJob.setStatus("ACTIVE".equals(etlTask.getStatus()) ? "0" : "1");
            sysJob.setUpdateBy(etlTask.getUpdateBy());
            sysJob.setUpdateTime(etlTask.getUpdateTime());
            sysJob.setRemark("ETL任务: " + etlTask.getName());

            sysJobService.updateJob(sysJob);
            log.info("更新Quartz任务成功: taskId={}", etlTask.getId());
        }
        catch (Exception e)
        {
            log.error("更新Quartz任务失败: taskId={}", etlTask.getId(), e);
            // 不抛出异常，允许ETL任务更新成功，但Quartz任务更新失败
        }
    }

    /**
     * 删除Quartz任务
     */
    private void deleteQuartzJob(Long taskId)
    {
        if (sysJobService == null)
        {
            log.warn("Quartz服务未配置，跳过删除Quartz任务: taskId={}", taskId);
            return;
        }

        try
        {
            dev.lhl.quartz.domain.SysJob sysJob = sysJobService.selectJobById(taskId);
            if (sysJob != null)
            {
                sysJobService.deleteJob(sysJob);
                log.info("删除Quartz任务成功: taskId={}", taskId);
            }
        }
        catch (Exception e)
        {
            log.error("删除Quartz任务失败: taskId={}", taskId, e);
            // 不抛出异常，允许ETL任务删除成功，但Quartz任务删除失败
        }
    }

    /**
     * 暂停Quartz任务
     */
    private void pauseQuartzJob(Long taskId)
    {
        if (sysJobService == null)
        {
            log.warn("Quartz服务未配置，跳过暂停Quartz任务: taskId={}", taskId);
            return;
        }

        try
        {
            dev.lhl.quartz.domain.SysJob sysJob = sysJobService.selectJobById(taskId);
            if (sysJob != null)
            {
                sysJob.setStatus("1"); // 1=暂停
                sysJobService.pauseJob(sysJob);
                log.info("暂停Quartz任务成功: taskId={}", taskId);
            }
        }
        catch (Exception e)
        {
            log.error("暂停Quartz任务失败: taskId={}", taskId, e);
            // 不抛出异常，允许ETL任务暂停成功，但Quartz任务暂停失败
        }
    }

    /**
     * 恢复Quartz任务
     */
    private void resumeQuartzJob(Long taskId)
    {
        if (sysJobService == null)
        {
            log.warn("Quartz服务未配置，跳过恢复Quartz任务: taskId={}", taskId);
            return;
        }

        try
        {
            dev.lhl.quartz.domain.SysJob sysJob = sysJobService.selectJobById(taskId);
            if (sysJob != null)
            {
                sysJob.setStatus("0"); // 0=正常
                sysJobService.resumeJob(sysJob);
                log.info("恢复Quartz任务成功: taskId={}", taskId);
            }
        }
        catch (Exception e)
        {
            log.error("恢复Quartz任务失败: taskId={}", taskId, e);
            // 不抛出异常，允许ETL任务恢复成功，但Quartz任务恢复失败
        }
    }
}
