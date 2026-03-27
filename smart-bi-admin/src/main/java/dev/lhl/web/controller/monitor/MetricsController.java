package dev.lhl.web.controller.monitor;

import dev.lhl.common.core.controller.BaseController;
import dev.lhl.common.core.domain.AjaxResult;
import dev.lhl.datasource.mapper.DataSourceMapper;
import dev.lhl.etl.mapper.EtlTaskMapper;
import dev.lhl.etl.mapper.EtlTaskExecutionMapper;
import dev.lhl.query.mapper.QueryRecordMapper;
import dev.lhl.dashboard.mapper.DashboardMapper;
import dev.lhl.system.mapper.SysUserMapper;
import dev.lhl.query.domain.QueryRecord;
import dev.lhl.query.service.IQueryCacheService;
import dev.lhl.query.service.ICacheWarmupService;
import dev.lhl.query.service.ISlowQueryService;
import dev.lhl.etl.domain.EtlTaskExecution;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 指标监控Controller
 * 提供系统指标、业务指标、性能指标
 * 
 * @author smart-bi
 */
@RestController
@RequestMapping("/api/monitor/metrics")
public class MetricsController extends BaseController
{
    @Autowired(required = false)
    private DataSourceMapper dataSourceMapper;
    
    @Autowired(required = false)
    private EtlTaskMapper etlTaskMapper;
    
    @Autowired(required = false)
    private QueryRecordMapper queryRecordMapper;
    
    @Autowired(required = false)
    private DashboardMapper dashboardMapper;
    
    @Autowired(required = false)
    private SysUserMapper userMapper;
    
    @Autowired(required = false)
    private EtlTaskExecutionMapper etlTaskExecutionMapper;

    @Autowired(required = false)
    private IQueryCacheService queryCacheService;

    @Autowired(required = false)
    private ICacheWarmupService cacheWarmupService;

    @Autowired(required = false)
    private ISlowQueryService slowQueryService;
    
    /**
     * 获取系统指标
     */
    @GetMapping("/system")
    public AjaxResult getSystemMetrics()
    {
        Map<String, Object> metrics = new HashMap<>();
        
        // JVM指标
        Runtime runtime = Runtime.getRuntime();
        metrics.put("jvm", Map.of(
            "totalMemory", runtime.totalMemory(),
            "freeMemory", runtime.freeMemory(),
            "usedMemory", runtime.totalMemory() - runtime.freeMemory(),
            "maxMemory", runtime.maxMemory(),
            "processors", runtime.availableProcessors()
        ));
        
        // 线程指标
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null)
        {
            rootGroup = rootGroup.getParent();
        }
        int threadCount = rootGroup.activeCount();
        metrics.put("threads", Map.of(
            "activeCount", threadCount
        ));
        
        return success(metrics);
    }
    
    /**
     * 获取业务指标
     */
    @GetMapping("/business")
    public AjaxResult getBusinessMetrics()
    {
        Map<String, Object> metrics = new HashMap<>();
        
        try
        {
            // 从数据库查询业务指标
            int dataSourceCount = 0;
            int etlTaskCount = 0;
            int queryRecordCount = 0;
            int dashboardCount = 0;
            int userCount = 0;
            
            if (dataSourceMapper != null)
            {
                dataSourceCount = dataSourceMapper.selectDataSourceList(null).size();
            }
            
            if (etlTaskMapper != null)
            {
                etlTaskCount = etlTaskMapper.selectEtlTaskList(null).size();
            }
            
            if (queryRecordMapper != null)
            {
                queryRecordCount = queryRecordMapper.selectQueryRecordList(null).size();
            }
            
            if (dashboardMapper != null)
            {
                dashboardCount = dashboardMapper.selectDashboardList(null).size();
            }
            
            if (userMapper != null)
            {
                userCount = userMapper.selectUserList(null).size();
            }
            
            metrics.put("dataSources", dataSourceCount);
            metrics.put("etlTasks", etlTaskCount);
            metrics.put("queryRecords", queryRecordCount);
            metrics.put("dashboards", dashboardCount);
            metrics.put("users", userCount);
        }
        catch (Exception e)
        {
            // 查询失败时返回0
            metrics.put("dataSources", 0);
            metrics.put("etlTasks", 0);
            metrics.put("queryRecords", 0);
            metrics.put("dashboards", 0);
            metrics.put("users", 0);
        }
        
        return success(metrics);
    }
    
    /**
     * 获取性能指标
     */
    @GetMapping("/performance")
    public AjaxResult getPerformanceMetrics()
    {
        Map<String, Object> metrics = new HashMap<>();
        
        try
        {
            // 1. 查询平均响应时间（从查询记录中计算）
            long queryAvgResponseTime = 0;
            double querySuccessRate = 100.0;
            
            if (queryRecordMapper != null)
            {
                List<QueryRecord> queryRecords = queryRecordMapper.selectQueryRecordList(null);
                if (queryRecords != null && !queryRecords.isEmpty())
                {
                    long totalDuration = 0;
                    int successCount = 0;
                    int totalCount = queryRecords.size();
                    
                    for (QueryRecord record : queryRecords)
                    {
                        if (record.getDuration() != null && record.getDuration() > 0)
                        {
                            totalDuration += record.getDuration();
                        }
                        if ("SUCCESS".equals(record.getStatus()))
                        {
                            successCount++;
                        }
                    }
                    
                    if (totalCount > 0)
                    {
                        queryAvgResponseTime = totalDuration / totalCount;
                        querySuccessRate = (double) successCount / totalCount * 100.0;
                    }
                }
            }
            
            // 2. ETL任务平均执行时间（从ETL执行记录中计算）
            long etlAvgExecutionTime = 0;
            
            if (etlTaskExecutionMapper != null)
            {
                List<EtlTaskExecution> executions = etlTaskExecutionMapper.selectEtlTaskExecutionList(null);
                if (executions != null && !executions.isEmpty())
                {
                    long totalExecutionTime = 0;
                    int count = 0;
                    
                    for (EtlTaskExecution execution : executions)
                    {
                        if (execution.getDuration() != null && execution.getDuration() > 0)
                        {
                            totalExecutionTime += execution.getDuration();
                            count++;
                        }
                    }
                    
                    if (count > 0)
                    {
                        etlAvgExecutionTime = totalExecutionTime / count;
                    }
                }
            }
            
            // 3. 系统负载（使用JVM可用处理器数作为参考）
            double systemLoad = 0.0;
            try
            {
                com.sun.management.OperatingSystemMXBean osBean = 
                    (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
                systemLoad = osBean.getProcessCpuLoad() * 100.0; // 转换为百分比
                if (systemLoad < 0)
                {
                    systemLoad = 0.0;
                }
            }
            catch (Exception e)
            {
                // 如果无法获取系统负载，使用0
                systemLoad = 0.0;
            }
            
            metrics.put("queryAvgResponseTime", queryAvgResponseTime);
            metrics.put("querySuccessRate", querySuccessRate);
            metrics.put("etlAvgExecutionTime", etlAvgExecutionTime);
            metrics.put("systemLoad", systemLoad);
        }
        catch (Exception e)
        {
            // 查询失败时返回默认值
            metrics.put("queryAvgResponseTime", 0);
            metrics.put("querySuccessRate", 100.0);
            metrics.put("etlAvgExecutionTime", 0);
            metrics.put("systemLoad", 0.0);
        }
        
        return success(metrics);
    }

    /**
     * 获取查询缓存命中率统计
     */
    @GetMapping("/cache/stats")
    public AjaxResult getCacheStats()
    {
        if (queryCacheService == null)
        {
            return success(Map.of("hitCount", 0, "missCount", 0, "hitRate", 0.0, "cacheSize", 0));
        }
        IQueryCacheService.HitRateStats stats = queryCacheService.getHitRateStats();
        return success(Map.of(
            "hitCount", stats.getHitCount(),
            "missCount", stats.getMissCount(),
            "hitRate", stats.getHitRate(),
            "cacheSize", stats.getCacheSize()
        ));
    }

    /**
     * 手动触发缓存预热
     */
    @GetMapping("/cache/warmup")
    public AjaxResult triggerCacheWarmup()
    {
        if (cacheWarmupService == null)
        {
            return error("缓存预热服务未配置");
        }
        int warmed = cacheWarmupService.warmupRecentQueries(20);
        return success(Map.of("warmedCount", warmed));
    }

    /**
     * 获取慢查询列表
     */
    @GetMapping("/slow-queries")
    public AjaxResult getSlowQueries()
    {
        if (slowQueryService == null)
        {
            return success(List.of());
        }
        List<Map<String, Object>> list = slowQueryService.getSlowQueries(5000, 50);
        return success(list);
    }
}
