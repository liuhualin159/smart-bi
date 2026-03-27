package dev.lhl.etl.service;

import dev.lhl.etl.domain.EtlTaskExecution;
import dev.lhl.etl.mapper.EtlTaskExecutionMapper;
import dev.lhl.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * DataX任务监控服务
 * 监控DataX任务执行状态，获取执行指标
 * 
 * @author smart-bi
 */
@Service
public class DataXMonitorService
{
    private static final Logger log = LoggerFactory.getLogger(DataXMonitorService.class);
    
    @Autowired
    private EtlTaskExecutionMapper executionMapper;
    
    /**
     * 获取任务执行状态
     * 从执行记录中获取任务状态
     * 
     * @param executionId 执行记录ID
     * @return 执行状态（RUNNING/SUCCESS/FAILED）
     */
    public String getExecutionStatus(Long executionId)
    {
        log.debug("获取任务执行状态: executionId={}", executionId);
        
        try
        {
            EtlTaskExecution execution = executionMapper.selectEtlTaskExecutionById(executionId);
            if (execution == null)
            {
                log.warn("执行记录不存在: executionId={}", executionId);
                return "UNKNOWN";
            }
            
            String status = execution.getStatus();
            if (StringUtils.isEmpty(status))
            {
                // 根据时间判断状态
                if (execution.getStartTime() != null && execution.getEndTime() == null)
                {
                    status = "RUNNING";
                }
                else if (execution.getEndTime() != null)
                {
                    status = StringUtils.isNotEmpty(execution.getErrorMessage()) ? "FAILED" : "SUCCESS";
                }
                else
                {
                    status = "UNKNOWN";
                }
            }
            
            log.debug("获取任务执行状态: executionId={}, status={}", executionId, status);
            return status;
        }
        catch (Exception e)
        {
            log.error("获取任务执行状态失败: executionId={}", executionId, e);
            return "UNKNOWN";
        }
    }
    
    /**
     * 获取任务执行指标
     * 从执行记录中获取执行指标（数据量、执行耗时、数据延迟等）
     * 
     * @param executionId 执行记录ID
     * @return 执行指标（dataCount, duration, dataLatency等）
     */
    public Map<String, Object> getExecutionMetrics(Long executionId)
    {
        log.debug("获取任务执行指标: executionId={}", executionId);
        
        try
        {
            EtlTaskExecution execution = executionMapper.selectEtlTaskExecutionById(executionId);
            if (execution == null)
            {
                log.warn("执行记录不存在: executionId={}", executionId);
                return createEmptyMetrics();
            }
            
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("dataCount", execution.getDataCount() != null ? execution.getDataCount() : 0L);
            metrics.put("duration", execution.getDuration() != null ? execution.getDuration() : 0L);
            
            // 计算数据延迟（当前时间 - 结束时间，如果任务已完成）
            if (execution.getEndTime() != null)
            {
                long dataLatency = System.currentTimeMillis() - execution.getEndTime().getTime();
                metrics.put("dataLatency", dataLatency);
            }
            else
            {
                metrics.put("dataLatency", 0L);
            }
            
            // 计算数据吞吐量（数据量/耗时，如果两者都有）
            if (execution.getDataCount() != null && execution.getDataCount() > 0 
                && execution.getDuration() != null && execution.getDuration() > 0)
            {
                double throughput = (double) execution.getDataCount() / (execution.getDuration() / 1000.0);
                metrics.put("throughput", throughput);
            }
            else
            {
                metrics.put("throughput", 0.0);
            }
            
            log.debug("获取任务执行指标: executionId={}, metrics={}", executionId, metrics);
            return metrics;
        }
        catch (Exception e)
        {
            log.error("获取任务执行指标失败: executionId={}", executionId, e);
            return createEmptyMetrics();
        }
    }
    
    /**
     * 创建空的指标Map
     */
    private Map<String, Object> createEmptyMetrics()
    {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("dataCount", 0L);
        metrics.put("duration", 0L);
        metrics.put("dataLatency", 0L);
        metrics.put("throughput", 0.0);
        return metrics;
    }
    
    /**
     * 获取任务执行日志
     * 从执行记录中获取执行日志（错误信息、断点信息等）
     * 
     * @param executionId 执行记录ID
     * @return 执行日志列表
     */
    public List<String> getExecutionLogs(Long executionId)
    {
        log.debug("获取任务执行日志: executionId={}", executionId);
        
        try
        {
            EtlTaskExecution execution = executionMapper.selectEtlTaskExecutionById(executionId);
            if (execution == null)
            {
                log.warn("执行记录不存在: executionId={}", executionId);
                return new ArrayList<>();
            }
            
            List<String> logs = new ArrayList<>();
            
            // 添加基本信息
            logs.add("执行记录ID: " + execution.getId());
            logs.add("任务ID: " + execution.getTaskId());
            logs.add("执行状态: " + execution.getStatus());
            
            if (execution.getStartTime() != null)
            {
                logs.add("开始时间: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(execution.getStartTime()));
            }
            
            if (execution.getEndTime() != null)
            {
                logs.add("结束时间: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(execution.getEndTime()));
            }
            
            if (execution.getDuration() != null)
            {
                logs.add("执行耗时: " + execution.getDuration() + "ms");
            }
            
            if (execution.getDataCount() != null)
            {
                logs.add("数据量: " + execution.getDataCount() + " 行");
            }
            
            if (StringUtils.isNotEmpty(execution.getErrorMessage()))
            {
                logs.add("错误信息: " + execution.getErrorMessage());
            }
            
            if (StringUtils.isNotEmpty(execution.getCheckpoint()))
            {
                logs.add("断点信息: " + execution.getCheckpoint());
            }
            
            log.debug("获取任务执行日志: executionId={}, logCount={}", executionId, logs.size());
            return logs;
        }
        catch (Exception e)
        {
            log.error("获取任务执行日志失败: executionId={}", executionId, e);
            return new ArrayList<>();
        }
    }
}
