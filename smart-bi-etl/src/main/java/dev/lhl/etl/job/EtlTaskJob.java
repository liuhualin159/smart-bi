package dev.lhl.etl.job;

import dev.lhl.etl.service.IEtlTaskService;
import dev.lhl.common.utils.spring.SpringUtils;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ETL任务Quartz Job
 * 通过Quartz调度执行ETL任务
 * 
 * @author smart-bi
 */
public class EtlTaskJob
{
    private static final Logger log = LoggerFactory.getLogger(EtlTaskJob.class);

    /**
     * 执行ETL任务
     * 此方法由Quartz Job调用
     * 
     * @param taskId 任务ID（从JobDataMap中获取）
     */
    public void execute(Long taskId)
    {
        try
        {
            log.info("Quartz触发ETL任务执行: taskId={}", taskId);
            
            IEtlTaskService etlTaskService = SpringUtils.getBean(IEtlTaskService.class);
            etlTaskService.executeEtlTask(taskId);
            
            log.info("ETL任务执行完成: taskId={}", taskId);
        }
        catch (Exception e)
        {
            log.error("ETL任务执行失败: taskId={}", taskId, e);
            throw new RuntimeException("ETL任务执行失败: " + e.getMessage(), e);
        }
    }
}
