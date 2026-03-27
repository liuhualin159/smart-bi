package dev.lhl.dashboard.mapper;

import dev.lhl.dashboard.domain.Dashboard;
import java.util.List;

/**
 * 看板Mapper接口
 * 
 * @author smart-bi
 */
public interface DashboardMapper
{
    Dashboard selectDashboardById(Long id);
    List<Dashboard> selectDashboardList(Dashboard dashboard);
    int insertDashboard(Dashboard dashboard);
    int updateDashboard(Dashboard dashboard);
    int deleteDashboardById(Long id);
    int deleteDashboardByIds(Long[] ids);
}
