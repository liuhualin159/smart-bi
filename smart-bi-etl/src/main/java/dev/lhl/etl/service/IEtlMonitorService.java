package dev.lhl.etl.service;

import dev.lhl.etl.domain.EtlTask;
import dev.lhl.etl.domain.EtlTaskExecution;
import java.util.List;
import java.util.Map;

/**
 * ETL任务监控服务接口
 * 提供ETL任务执行状态监控、指标统计等功能
 * 
 * @author smart-bi
 */
public interface IEtlMonitorService
{
    /**
     * 获取所有ETL任务的执行状态概览
     * 
     * @return 任务状态统计（运行中、成功、失败数量等）
     */
    Map<String, Object> getTaskStatusOverview();
    
    /**
     * 获取指定任务的执行状态
     * 
     * @param taskId 任务ID
     * @return 任务执行状态信息
     */
    Map<String, Object> getTaskStatus(Long taskId);
    
    /**
     * 获取任务执行记录列表
     * 
     * @param taskId 任务ID（可选，为null时查询所有任务）
     * @param status 执行状态（可选）
     * @param limit 限制返回数量
     * @return 执行记录列表
     */
    List<EtlTaskExecution> getExecutionList(Long taskId, String status, Integer limit);
    
    /**
     * 获取任务监控数据
     * 包括：执行成功率、平均执行时间、数据量统计等
     * 
     * @param taskId 任务ID（可选，为null时统计所有任务）
     * @param days 统计天数（默认7天）
     * @return 监控数据
     */
    Map<String, Object> getMonitorData(Long taskId, Integer days);
    
    /**
     * 获取任务执行趋势数据
     * 
     * @param taskId 任务ID
     * @param days 统计天数
     * @return 趋势数据（按日期分组）
     */
    List<Map<String, Object>> getExecutionTrend(Long taskId, Integer days);
    
    /**
     * 获取实时运行中的任务列表
     * 
     * @return 运行中的任务列表
     */
    List<EtlTask> getRunningTasks();
}
