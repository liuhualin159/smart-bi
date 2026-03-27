package dev.lhl.dashboard.service;

import dev.lhl.dashboard.domain.Dashboard;
import java.util.List;

/**
 * 看板Service接口
 * 
 * @author smart-bi
 */
public interface IDashboardService
{
    Dashboard selectDashboardById(Long id);
    List<Dashboard> selectDashboardList(Dashboard dashboard);
    int insertDashboard(Dashboard dashboard);
    int updateDashboard(Dashboard dashboard);
    int deleteDashboardByIds(Long[] ids);
}
