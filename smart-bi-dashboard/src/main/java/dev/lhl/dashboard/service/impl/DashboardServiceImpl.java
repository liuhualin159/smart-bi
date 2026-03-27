package dev.lhl.dashboard.service.impl;

import dev.lhl.dashboard.domain.Dashboard;
import dev.lhl.dashboard.mapper.DashboardMapper;
import dev.lhl.dashboard.service.IDashboardService;
import dev.lhl.common.utils.DateUtils;
import dev.lhl.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 看板Service业务层处理
 * 
 * @author smart-bi
 */
@Service
public class DashboardServiceImpl implements IDashboardService
{
    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    @Autowired
    private DashboardMapper dashboardMapper;

    @Override
    public Dashboard selectDashboardById(Long id)
    {
        return dashboardMapper.selectDashboardById(id);
    }

    @Override
    public List<Dashboard> selectDashboardList(Dashboard dashboard)
    {
        return dashboardMapper.selectDashboardList(dashboard);
    }

    @Override
    @Transactional
    public int insertDashboard(Dashboard dashboard)
    {
        dashboard.setUserId(SecurityUtils.getUserId());
        dashboard.setCreateBy(SecurityUtils.getUsername());
        dashboard.setCreateTime(DateUtils.getNowDate());
        int result = dashboardMapper.insertDashboard(dashboard);
        log.info("新增看板成功: dashboardId={}, dashboardName={}", dashboard.getId(), dashboard.getName());
        return result;
    }

    @Override
    @Transactional
    public int updateDashboard(Dashboard dashboard)
    {
        dashboard.setUpdateBy(SecurityUtils.getUsername());
        dashboard.setUpdateTime(DateUtils.getNowDate());
        int result = dashboardMapper.updateDashboard(dashboard);
        log.info("修改看板成功: dashboardId={}", dashboard.getId());
        return result;
    }

    @Override
    @Transactional
    public int deleteDashboardByIds(Long[] ids)
    {
        int result = dashboardMapper.deleteDashboardByIds(ids);
        log.info("批量删除看板成功: ids={}", ids);
        return result;
    }
}
