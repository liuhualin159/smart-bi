package dev.lhl.etl.service;

import dev.lhl.etl.domain.EtlTask;
import dev.lhl.etl.domain.EtlTaskExecution;

/**
 * ETL任务告警服务接口
 * 提供ETL任务执行失败、超时等异常情况的告警功能
 * 
 * @author smart-bi
 */
public interface IEtlAlertService
{
    /**
     * 检查并发送任务执行失败告警
     * 
     * @param task 任务信息
     * @param execution 执行记录
     */
    void checkAndSendFailureAlert(EtlTask task, EtlTaskExecution execution);
    
    /**
     * 检查并发送任务执行超时告警
     * 
     * @param task 任务信息
     * @param execution 执行记录
     * @param timeoutMinutes 超时时间（分钟）
     */
    void checkAndSendTimeoutAlert(EtlTask task, EtlTaskExecution execution, int timeoutMinutes);
    
    /**
     * 检查并发送任务连续失败告警
     * 
     * @param task 任务信息
     * @param consecutiveFailures 连续失败次数
     */
    void checkAndSendConsecutiveFailureAlert(EtlTask task, int consecutiveFailures);
    
    /**
     * 发送自定义告警消息
     * 
     * @param taskId 任务ID
     * @param alertType 告警类型（FAILURE/TIMEOUT/CONSECUTIVE_FAILURE）
     * @param message 告警消息
     */
    void sendAlert(Long taskId, String alertType, String message);
}
