package dev.lhl.dashboard.service;

import dev.lhl.dashboard.domain.ChartCard;
import java.util.List;

/**
 * 图表卡片Service接口
 * 
 * @author smart-bi
 */
public interface IChartCardService
{
    ChartCard selectChartCardById(Long id);
    List<ChartCard> selectChartCardList(ChartCard chartCard);
    int insertChartCard(ChartCard chartCard);
    int updateChartCard(ChartCard chartCard);
    int deleteChartCardByIds(Long[] ids);
}
