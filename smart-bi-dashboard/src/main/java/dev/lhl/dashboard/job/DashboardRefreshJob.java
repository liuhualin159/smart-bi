package dev.lhl.dashboard.job;

import dev.lhl.dashboard.service.IDashboardRefreshService;
import dev.lhl.common.utils.spring.SpringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 看板刷新Quartz Job
 * 通过Quartz调度执行看板刷新任务
 * 
 * @author smart-bi
 */
public class DashboardRefreshJob
{
    private static final Logger log = LoggerFactory.getLogger(DashboardRefreshJob.class);
    
    /**
     * 执行看板刷新
     * 此方法由Quartz Job调用
     * 
     * @param dashboardId 看板ID（从JobDataMap中获取）
     */
    public void execute(Long dashboardId)
    {
        try
        {
            log.info("Quartz触发看板刷新: dashboardId={}", dashboardId);
            
            IDashboardRefreshService refreshService = SpringUtils.getBean(IDashboardRefreshService.class);
            IDashboardRefreshService.RefreshResult result = refreshService.refreshDashboard(dashboardId);
            
            log.info("看板刷新完成: dashboardId={}, successCount={}, failCount={}, message={}", 
                dashboardId, result.getSuccessCount(), result.getFailCount(), result.getMessage());
        }
        catch (Exception e)
        {
            log.error("看板刷新失败: dashboardId={}", dashboardId, e);
            throw new RuntimeException("看板刷新失败: " + e.getMessage(), e);
        }
    }
}
