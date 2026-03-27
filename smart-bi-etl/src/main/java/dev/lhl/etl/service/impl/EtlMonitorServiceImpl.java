package dev.lhl.etl.service.impl;

import dev.lhl.etl.domain.EtlTask;
import dev.lhl.etl.domain.EtlTaskExecution;
import dev.lhl.etl.mapper.EtlTaskExecutionMapper;
import dev.lhl.etl.mapper.EtlTaskMapper;
import dev.lhl.etl.service.IEtlMonitorService;
import dev.lhl.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ETL任务监控服务实现
 * 
 * @author smart-bi
 */
@Service
public class EtlMonitorServiceImpl implements IEtlMonitorService
{
    private static final Logger log = LoggerFactory.getLogger(EtlMonitorServiceImpl.class);
    
    @Autowired
    private EtlTaskMapper etlTaskMapper;
    
    @Autowired
    private EtlTaskExecutionMapper etlTaskExecutionMapper;
    
    @Override
    public Map<String, Object> getTaskStatusOverview()
    {
        log.debug("获取ETL任务状态概览");
        
        try
        {
            // 查询所有任务
            List<EtlTask> allTasks = etlTaskMapper.selectEtlTaskList(new EtlTask());
            
            // 统计各状态任务数量
            long totalCount = allTasks.size();
            long activeCount = allTasks.stream()
                .filter(task -> "ACTIVE".equals(task.getStatus()))
                .count();
            long inactiveCount = allTasks.stream()
                .filter(task -> "INACTIVE".equals(task.getStatus()))
                .count();
            long pausedCount = allTasks.stream()
                .filter(task -> "PAUSED".equals(task.getStatus()))
                .count();
            
            // 查询最近24小时的执行记录
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -1);
            Date yesterday = cal.getTime();
            
            List<EtlTaskExecution> recentExecutions = etlTaskExecutionMapper.selectRecentExecutions(yesterday);
            
            // 统计执行状态
            long runningCount = recentExecutions.stream()
                .filter(exec -> "RUNNING".equals(exec.getStatus()))
                .count();
            long successCount = recentExecutions.stream()
                .filter(exec -> "SUCCESS".equals(exec.getStatus()))
                .count();
            long failedCount = recentExecutions.stream()
                .filter(exec -> "FAILED".equals(exec.getStatus()))
                .count();
            
            Map<String, Object> overview = new HashMap<>();
            overview.put("totalTasks", totalCount);
            overview.put("activeTasks", activeCount);
            overview.put("inactiveTasks", inactiveCount);
            overview.put("pausedTasks", pausedCount);
            overview.put("runningExecutions", runningCount);
            overview.put("successExecutions", successCount);
            overview.put("failedExecutions", failedCount);
            
            // 计算成功率
            long totalExecutions = successCount + failedCount;
            double successRate = totalExecutions > 0 ? (double) successCount / totalExecutions * 100 : 0.0;
            overview.put("successRate", String.format("%.2f", successRate));
            
            log.debug("任务状态概览: {}", overview);
            return overview;
        }
        catch (Exception e)
        {
            log.error("获取任务状态概览失败", e);
            throw new RuntimeException("获取任务状态概览失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, Object> getTaskStatus(Long taskId)
    {
        log.debug("获取任务执行状态: taskId={}", taskId);
        
        try
        {
            EtlTask task = etlTaskMapper.selectEtlTaskById(taskId);
            if (task == null)
            {
                throw new RuntimeException("任务不存在: " + taskId);
            }
            
            // 查询最近的执行记录
            List<EtlTaskExecution> executions = etlTaskExecutionMapper.selectEtlTaskExecutionListByTaskId(taskId);
            
            Map<String, Object> status = new HashMap<>();
            status.put("taskId", taskId);
            status.put("taskName", task.getName());
            status.put("taskStatus", task.getStatus());
            status.put("lastRunTime", task.getLastRunTime());
            status.put("nextRunTime", task.getNextRunTime());
            
            // 获取最后一次执行记录
            if (!executions.isEmpty())
            {
                EtlTaskExecution lastExecution = executions.get(0);
                status.put("lastExecutionStatus", lastExecution.getStatus());
                status.put("lastExecutionStartTime", lastExecution.getStartTime());
                status.put("lastExecutionEndTime", lastExecution.getEndTime());
                status.put("lastExecutionDuration", lastExecution.getDuration());
                status.put("lastExecutionDataCount", lastExecution.getDataCount());
                status.put("lastExecutionErrorMessage", lastExecution.getErrorMessage());
            }
            
            // 统计执行次数
            long totalExecutions = executions.size();
            long successCount = executions.stream()
                .filter(exec -> "SUCCESS".equals(exec.getStatus()))
                .count();
            long failedCount = executions.stream()
                .filter(exec -> "FAILED".equals(exec.getStatus()))
                .count();
            
            status.put("totalExecutions", totalExecutions);
            status.put("successCount", successCount);
            status.put("failedCount", failedCount);
            
            // 计算平均执行时间
            OptionalDouble avgDuration = executions.stream()
                .filter(exec -> exec.getDuration() != null)
                .mapToLong(EtlTaskExecution::getDuration)
                .average();
            status.put("avgDuration", avgDuration.isPresent() ? (long) avgDuration.getAsDouble() : 0L);
            
            log.debug("任务执行状态: {}", status);
            return status;
        }
        catch (Exception e)
        {
            log.error("获取任务执行状态失败: taskId={}", taskId, e);
            throw new RuntimeException("获取任务执行状态失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<EtlTaskExecution> getExecutionList(Long taskId, String status, Integer limit)
    {
        log.debug("获取执行记录列表: taskId={}, status={}, limit={}", taskId, status, limit);
        
        try
        {
            // 使用支持分页的查询方法，让PageHelper在SQL层面进行分页
            EtlTaskExecution queryParam = new EtlTaskExecution();
            if (taskId != null)
            {
                queryParam.setTaskId(taskId);
            }
            if (StringUtils.isNotEmpty(status))
            {
                queryParam.setStatus(status);
            }
            
            // 使用selectEtlTaskExecutionList方法，支持分页和条件查询
            List<EtlTaskExecution> executions = etlTaskExecutionMapper.selectEtlTaskExecutionList(queryParam);
            
            // 注意：limit参数在Controller中通过PageHelper的startPage()处理，这里不再需要手动限制
            // 如果需要在Service层限制，可以在分页后处理，但不推荐
            
            log.debug("执行记录列表数量: {}", executions.size());
            return executions;
        }
        catch (Exception e)
        {
            log.error("获取执行记录列表失败: taskId={}, status={}", taskId, status, e);
            throw new RuntimeException("获取执行记录列表失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, Object> getMonitorData(Long taskId, Integer days)
    {
        log.debug("获取监控数据: taskId={}, days={}", taskId, days);
        
        try
        {
            if (days == null || days <= 0)
            {
                days = 7; // 默认7天
            }
            
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -days);
            Date startDate = cal.getTime();
            
            List<EtlTaskExecution> executions;
            if (taskId != null)
            {
                executions = etlTaskExecutionMapper.selectEtlTaskExecutionListByTaskId(taskId);
                // 过滤日期范围
                executions = executions.stream()
                    .filter(exec -> exec.getCreateTime() != null && exec.getCreateTime().after(startDate))
                    .collect(Collectors.toList());
            }
            else
            {
                executions = etlTaskExecutionMapper.selectRecentExecutions(startDate);
            }
            
            Map<String, Object> monitorData = new HashMap<>();
            
            // 统计执行次数
            long totalExecutions = executions.size();
            long successCount = executions.stream()
                .filter(exec -> "SUCCESS".equals(exec.getStatus()))
                .count();
            long failedCount = executions.stream()
                .filter(exec -> "FAILED".equals(exec.getStatus()))
                .count();
            long runningCount = executions.stream()
                .filter(exec -> "RUNNING".equals(exec.getStatus()))
                .count();
            
            monitorData.put("totalExecutions", totalExecutions);
            monitorData.put("successCount", successCount);
            monitorData.put("failedCount", failedCount);
            monitorData.put("runningCount", runningCount);
            
            // 计算成功率
            long completedExecutions = successCount + failedCount;
            double successRate = completedExecutions > 0 ? (double) successCount / completedExecutions * 100 : 0.0;
            monitorData.put("successRate", String.format("%.2f", successRate));
            
            // 统计数据量
            long totalDataCount = executions.stream()
                .filter(exec -> exec.getDataCount() != null)
                .mapToLong(EtlTaskExecution::getDataCount)
                .sum();
            monitorData.put("totalDataCount", totalDataCount);
            
            // 计算平均执行时间
            OptionalDouble avgDuration = executions.stream()
                .filter(exec -> exec.getDuration() != null && exec.getDuration() > 0)
                .mapToLong(EtlTaskExecution::getDuration)
                .average();
            monitorData.put("avgDuration", avgDuration.isPresent() ? (long) avgDuration.getAsDouble() : 0L);
            
            // 计算最大/最小执行时间
            OptionalLong maxDuration = executions.stream()
                .filter(exec -> exec.getDuration() != null && exec.getDuration() > 0)
                .mapToLong(EtlTaskExecution::getDuration)
                .max();
            OptionalLong minDuration = executions.stream()
                .filter(exec -> exec.getDuration() != null && exec.getDuration() > 0)
                .mapToLong(EtlTaskExecution::getDuration)
                .min();
            
            monitorData.put("maxDuration", maxDuration.isPresent() ? maxDuration.getAsLong() : 0L);
            monitorData.put("minDuration", minDuration.isPresent() ? minDuration.getAsLong() : 0L);
            
            log.debug("监控数据: {}", monitorData);
            return monitorData;
        }
        catch (Exception e)
        {
            log.error("获取监控数据失败: taskId={}, days={}", taskId, days, e);
            throw new RuntimeException("获取监控数据失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<Map<String, Object>> getExecutionTrend(Long taskId, Integer days)
    {
        log.debug("获取执行趋势数据: taskId={}, days={}", taskId, days);
        
        try
        {
            if (days == null || days <= 0)
            {
                days = 7; // 默认7天
            }
            
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -days);
            Date startDate = cal.getTime();
            
            List<EtlTaskExecution> executions;
            if (taskId != null)
            {
                executions = etlTaskExecutionMapper.selectEtlTaskExecutionListByTaskId(taskId);
                executions = executions.stream()
                    .filter(exec -> exec.getCreateTime() != null && exec.getCreateTime().after(startDate))
                    .collect(Collectors.toList());
            }
            else
            {
                executions = etlTaskExecutionMapper.selectRecentExecutions(startDate);
            }
            
            // 按日期分组统计
            Map<String, Map<String, Object>> dailyStats = new LinkedHashMap<>();
            
            Calendar dateCal = Calendar.getInstance();
            for (int i = 0; i < days; i++)
            {
                dateCal.setTime(startDate);
                dateCal.add(Calendar.DAY_OF_MONTH, i);
                dateCal.set(Calendar.HOUR_OF_DAY, 0);
                dateCal.set(Calendar.MINUTE, 0);
                dateCal.set(Calendar.SECOND, 0);
                dateCal.set(Calendar.MILLISECOND, 0);
                
                Date dayStart = dateCal.getTime();
                dateCal.add(Calendar.DAY_OF_MONTH, 1);
                Date dayEnd = dateCal.getTime();
                
                String dateKey = String.format("%04d-%02d-%02d", 
                    dateCal.get(Calendar.YEAR),
                    dateCal.get(Calendar.MONTH) + 1,
                    dateCal.get(Calendar.DAY_OF_MONTH));
                
                final Date finalDayStart = dayStart;
                final Date finalDayEnd = dayEnd;
                
                List<EtlTaskExecution> dayExecutions = executions.stream()
                    .filter(exec -> {
                        Date execTime = exec.getCreateTime();
                        return execTime != null && execTime.after(finalDayStart) && execTime.before(finalDayEnd);
                    })
                    .collect(Collectors.toList());
                
                Map<String, Object> dayStat = new HashMap<>();
                dayStat.put("date", dateKey);
                dayStat.put("total", dayExecutions.size());
                dayStat.put("success", dayExecutions.stream().filter(e -> "SUCCESS".equals(e.getStatus())).count());
                dayStat.put("failed", dayExecutions.stream().filter(e -> "FAILED".equals(e.getStatus())).count());
                dayStat.put("dataCount", dayExecutions.stream()
                    .filter(e -> e.getDataCount() != null)
                    .mapToLong(EtlTaskExecution::getDataCount)
                    .sum());
                
                dailyStats.put(dateKey, dayStat);
            }
            
            List<Map<String, Object>> trend = new ArrayList<>(dailyStats.values());
            log.debug("执行趋势数据条数: {}", trend.size());
            return trend;
        }
        catch (Exception e)
        {
            log.error("获取执行趋势数据失败: taskId={}, days={}", taskId, days, e);
            throw new RuntimeException("获取执行趋势数据失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<EtlTask> getRunningTasks()
    {
        log.debug("获取运行中的任务列表");
        
        try
        {
            // 查询所有活跃任务
            EtlTask query = new EtlTask();
            query.setStatus("ACTIVE");
            List<EtlTask> activeTasks = etlTaskMapper.selectEtlTaskList(query);
            
            // 过滤出正在运行的任务（有RUNNING状态的执行记录）
            List<EtlTask> runningTasks = new ArrayList<>();
            for (EtlTask task : activeTasks)
            {
                List<EtlTaskExecution> executions = etlTaskExecutionMapper.selectEtlTaskExecutionListByTaskId(task.getId());
                boolean isRunning = executions.stream()
                    .anyMatch(exec -> "RUNNING".equals(exec.getStatus()));
                
                if (isRunning)
                {
                    runningTasks.add(task);
                }
            }
            
            log.debug("运行中的任务数量: {}", runningTasks.size());
            return runningTasks;
        }
        catch (Exception e)
        {
            log.error("获取运行中的任务列表失败", e);
            throw new RuntimeException("获取运行中的任务列表失败: " + e.getMessage(), e);
        }
    }
}
