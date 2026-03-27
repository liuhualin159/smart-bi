package dev.lhl.dashboard.mapper;

import dev.lhl.dashboard.domain.ChartCard;
import java.util.List;

/**
 * 图表卡片Mapper接口
 * 
 * @author smart-bi
 */
public interface ChartCardMapper
{
    ChartCard selectChartCardById(Long id);
    List<ChartCard> selectChartCardList(ChartCard chartCard);
    int insertChartCard(ChartCard chartCard);
    int updateChartCard(ChartCard chartCard);
    int deleteChartCardById(Long id);
    int deleteChartCardByIds(Long[] ids);
}
