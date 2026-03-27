package dev.lhl.dashboard.mapper;

import dev.lhl.dashboard.domain.DashboardCard;
import java.util.List;

/**
 * 看板卡片关联Mapper接口
 * 
 * @author smart-bi
 */
public interface DashboardCardMapper
{
    List<DashboardCard> selectDashboardCardListByDashboardId(Long dashboardId);
    int insertDashboardCard(DashboardCard dashboardCard);
    int updateDashboardCard(DashboardCard dashboardCard);
    int deleteDashboardCardById(Long id);
    int deleteDashboardCardByDashboardId(Long dashboardId);
    List<DashboardCard> selectChildCards(Long parentId);
}
