package dev.lhl.etl.service.impl;

import dev.lhl.etl.domain.EtlTask;
import dev.lhl.etl.domain.EtlTaskExecution;
import dev.lhl.etl.mapper.EtlTaskExecutionMapper;
import dev.lhl.etl.service.IEtlAlertService;
import dev.lhl.system.domain.SysNotice;
import dev.lhl.system.service.ISysNoticeService;
import dev.lhl.common.utils.DateUtils;
import dev.lhl.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * ETL任务告警服务实现
 * 集成若依框架的通知功能实现告警（5分钟内送达）
 * 
 * @author smart-bi
 */
@Service
public class EtlAlertServiceImpl implements IEtlAlertService
{
    private static final Logger log = LoggerFactory.getLogger(EtlAlertServiceImpl.class);
    
    @Autowired(required = false)
    private ISysNoticeService noticeService;
    
    @Autowired
    private EtlTaskExecutionMapper executionMapper;
    
    // 默认超时时间：30分钟
    private static final int DEFAULT_TIMEOUT_MINUTES = 30;
    
    // 连续失败告警阈值：3次
    private static final int CONSECUTIVE_FAILURE_THRESHOLD = 3;
    
    @Override
    public void checkAndSendFailureAlert(EtlTask task, EtlTaskExecution execution)
    {
        if (task == null || execution == null)
        {
            log.warn("任务或执行记录为空，跳过告警");
            return;
        }
        
        if (!"FAILED".equals(execution.getStatus()))
        {
            log.debug("执行状态不是失败，跳过告警: status={}", execution.getStatus());
            return;
        }
        
        try
        {
            String alertTitle = String.format("ETL任务执行失败告警 - %s", task.getName());
            String alertContent = buildFailureAlertContent(task, execution);
            
            sendSystemNotice(alertTitle, alertContent, "FAILURE");
            
            log.info("任务执行失败告警已发送: taskId={}, taskName={}", task.getId(), task.getName());
        }
        catch (Exception e)
        {
            log.error("发送任务执行失败告警失败: taskId={}", task.getId(), e);
        }
    }
    
    @Override
    public void checkAndSendTimeoutAlert(EtlTask task, EtlTaskExecution execution, int timeoutMinutes)
    {
        if (task == null || execution == null)
        {
            log.warn("任务或执行记录为空，跳过超时告警");
            return;
        }
        
        if (!"RUNNING".equals(execution.getStatus()))
        {
            log.debug("执行状态不是运行中，跳过超时告警: status={}", execution.getStatus());
            return;
        }
        
        if (timeoutMinutes <= 0)
        {
            timeoutMinutes = DEFAULT_TIMEOUT_MINUTES;
        }
        
        try
        {
            // 检查是否超时
            Date startTime = execution.getStartTime();
            if (startTime == null)
            {
                log.warn("执行开始时间为空，无法判断超时: executionId={}", execution.getId());
                return;
            }
            
            long elapsedMinutes = (System.currentTimeMillis() - startTime.getTime()) / (1000 * 60);
            if (elapsedMinutes < timeoutMinutes)
            {
                log.debug("任务执行未超时: elapsedMinutes={}, timeoutMinutes={}", elapsedMinutes, timeoutMinutes);
                return;
            }
            
            String alertTitle = String.format("ETL任务执行超时告警 - %s", task.getName());
            String alertContent = buildTimeoutAlertContent(task, execution, elapsedMinutes);
            
            sendSystemNotice(alertTitle, alertContent, "TIMEOUT");
            
            log.info("任务执行超时告警已发送: taskId={}, elapsedMinutes={}", task.getId(), elapsedMinutes);
        }
        catch (Exception e)
        {
            log.error("发送任务执行超时告警失败: taskId={}", task.getId(), e);
        }
    }
    
    @Override
    public void checkAndSendConsecutiveFailureAlert(EtlTask task, int consecutiveFailures)
    {
        if (task == null)
        {
            log.warn("任务为空，跳过连续失败告警");
            return;
        }
        
        if (consecutiveFailures < CONSECUTIVE_FAILURE_THRESHOLD)
        {
            log.debug("连续失败次数未达到阈值: consecutiveFailures={}, threshold={}", 
                consecutiveFailures, CONSECUTIVE_FAILURE_THRESHOLD);
            return;
        }
        
        try
        {
            // 查询最近的执行记录验证连续失败
            List<EtlTaskExecution> recentExecutions = executionMapper.selectEtlTaskExecutionListByTaskId(task.getId());
            if (recentExecutions.size() < consecutiveFailures)
            {
                log.debug("执行记录数量不足: count={}, required={}", recentExecutions.size(), consecutiveFailures);
                return;
            }
            
            // 检查是否真的连续失败
            boolean allFailed = true;
            for (int i = 0; i < consecutiveFailures && i < recentExecutions.size(); i++)
            {
                if (!"FAILED".equals(recentExecutions.get(i).getStatus()))
                {
                    allFailed = false;
                    break;
                }
            }
            
            if (!allFailed)
            {
                log.debug("不是连续失败，跳过告警");
                return;
            }
            
            String alertTitle = String.format("ETL任务连续失败告警 - %s", task.getName());
            String alertContent = buildConsecutiveFailureAlertContent(task, consecutiveFailures);
            
            sendSystemNotice(alertTitle, alertContent, "CONSECUTIVE_FAILURE");
            
            log.info("任务连续失败告警已发送: taskId={}, consecutiveFailures={}", task.getId(), consecutiveFailures);
        }
        catch (Exception e)
        {
            log.error("发送任务连续失败告警失败: taskId={}", task.getId(), e);
        }
    }
    
    @Override
    public void sendAlert(Long taskId, String alertType, String message)
    {
        if (taskId == null || StringUtils.isEmpty(message))
        {
            log.warn("任务ID或消息为空，跳过告警");
            return;
        }
        
        try
        {
            String alertTitle = String.format("ETL任务告警 - 任务ID: %d", taskId);
            String alertContent = String.format("告警类型: %s\n\n%s", alertType, message);
            
            sendSystemNotice(alertTitle, alertContent, alertType);
            
            log.info("自定义告警已发送: taskId={}, alertType={}", taskId, alertType);
        }
        catch (Exception e)
        {
            log.error("发送自定义告警失败: taskId={}, alertType={}", taskId, alertType, e);
        }
    }
    
    /**
     * 发送系统通知
     * 使用若依框架的通知功能，确保5分钟内送达
     */
    private void sendSystemNotice(String title, String content, String noticeType)
    {
        if (noticeService == null)
        {
            log.warn("通知服务未配置，无法发送告警: title={}", title);
            return;
        }
        
        try
        {
            SysNotice notice = new SysNotice();
            notice.setNoticeTitle(title);
            notice.setNoticeContent(content);
            notice.setNoticeType(noticeType);
            notice.setStatus("0"); // 0-正常，1-关闭
            notice.setCreateTime(DateUtils.getNowDate());
            notice.setCreateBy("system");
            
            int result = noticeService.insertNotice(notice);
            if (result > 0)
            {
                log.info("系统通知已创建: title={}", title);
            }
            else
            {
                log.warn("系统通知创建失败: title={}", title);
            }
        }
        catch (Exception e)
        {
            log.error("创建系统通知失败: title={}", title, e);
            throw new RuntimeException("创建系统通知失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建失败告警内容
     */
    private String buildFailureAlertContent(EtlTask task, EtlTaskExecution execution)
    {
        StringBuilder content = new StringBuilder();
        content.append("任务名称: ").append(task.getName()).append("\n");
        content.append("任务ID: ").append(task.getId()).append("\n");
        content.append("执行记录ID: ").append(execution.getId()).append("\n");
        content.append("执行开始时间: ").append(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, execution.getStartTime())).append("\n");
        
        if (execution.getEndTime() != null)
        {
            content.append("执行结束时间: ").append(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, execution.getEndTime())).append("\n");
        }
        
        if (execution.getDuration() != null)
        {
            content.append("执行耗时: ").append(execution.getDuration()).append(" 毫秒\n");
        }
        
        if (StringUtils.isNotEmpty(execution.getErrorMessage()))
        {
            content.append("错误信息: ").append(execution.getErrorMessage()).append("\n");
        }
        
        content.append("\n请及时检查任务配置和数据源连接状态。");
        return content.toString();
    }
    
    /**
     * 构建超时告警内容
     */
    private String buildTimeoutAlertContent(EtlTask task, EtlTaskExecution execution, long elapsedMinutes)
    {
        StringBuilder content = new StringBuilder();
        content.append("任务名称: ").append(task.getName()).append("\n");
        content.append("任务ID: ").append(task.getId()).append("\n");
        content.append("执行记录ID: ").append(execution.getId()).append("\n");
        content.append("执行开始时间: ").append(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, execution.getStartTime())).append("\n");
        content.append("已执行时长: ").append(elapsedMinutes).append(" 分钟\n");
        content.append("任务状态: 运行中（可能已超时）\n");
        content.append("\n请检查任务执行情况，必要时手动停止任务。");
        return content.toString();
    }
    
    /**
     * 构建连续失败告警内容
     */
    private String buildConsecutiveFailureAlertContent(EtlTask task, int consecutiveFailures)
    {
        StringBuilder content = new StringBuilder();
        content.append("任务名称: ").append(task.getName()).append("\n");
        content.append("任务ID: ").append(task.getId()).append("\n");
        content.append("连续失败次数: ").append(consecutiveFailures).append(" 次\n");
        content.append("告警阈值: ").append(CONSECUTIVE_FAILURE_THRESHOLD).append(" 次\n");
        content.append("\n任务已连续失败多次，请立即检查任务配置、数据源连接和网络状态。");
        return content.toString();
    }
}
