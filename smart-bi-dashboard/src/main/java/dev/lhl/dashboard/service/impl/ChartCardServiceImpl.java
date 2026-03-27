package dev.lhl.dashboard.service.impl;

import dev.lhl.dashboard.domain.ChartCard;
import dev.lhl.dashboard.mapper.ChartCardMapper;
import dev.lhl.dashboard.service.IChartCardService;
import dev.lhl.common.utils.DateUtils;
import dev.lhl.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 图表卡片Service业务层处理
 * 
 * @author smart-bi
 */
@Service
public class ChartCardServiceImpl implements IChartCardService
{
    private static final Logger log = LoggerFactory.getLogger(ChartCardServiceImpl.class);

    @Autowired
    private ChartCardMapper chartCardMapper;

    @Override
    public ChartCard selectChartCardById(Long id)
    {
        return chartCardMapper.selectChartCardById(id);
    }

    @Override
    public List<ChartCard> selectChartCardList(ChartCard chartCard)
    {
        return chartCardMapper.selectChartCardList(chartCard);
    }

    @Override
    @Transactional
    public int insertChartCard(ChartCard chartCard)
    {
        chartCard.setUserId(SecurityUtils.getUserId());
        chartCard.setCreateBy(SecurityUtils.getUsername());
        chartCard.setCreateTime(DateUtils.getNowDate());
        int result = chartCardMapper.insertChartCard(chartCard);
        log.info("新增图表卡片成功: cardId={}, cardName={}", chartCard.getId(), chartCard.getName());
        return result;
    }

    @Override
    @Transactional
    public int updateChartCard(ChartCard chartCard)
    {
        chartCard.setUpdateBy(SecurityUtils.getUsername());
        chartCard.setUpdateTime(DateUtils.getNowDate());
        int result = chartCardMapper.updateChartCard(chartCard);
        log.info("修改图表卡片成功: cardId={}", chartCard.getId());
        return result;
    }

    @Override
    @Transactional
    public int deleteChartCardByIds(Long[] ids)
    {
        int result = chartCardMapper.deleteChartCardByIds(ids);
        log.info("批量删除图表卡片成功: ids={}", ids);
        return result;
    }
}
