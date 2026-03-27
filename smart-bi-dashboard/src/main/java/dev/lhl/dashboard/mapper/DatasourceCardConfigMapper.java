package dev.lhl.dashboard.mapper;

import dev.lhl.dashboard.domain.DatasourceCardConfig;
import java.util.List;

/**
 * 数据源卡片配置Mapper接口
 * 
 * @author smart-bi
 */
public interface DatasourceCardConfigMapper
{
    DatasourceCardConfig selectDatasourceCardConfigById(Long id);

    DatasourceCardConfig selectByDashboardCardId(Long dashboardCardId);

    List<DatasourceCardConfig> selectDatasourceCardConfigList(DatasourceCardConfig config);

    int insertDatasourceCardConfig(DatasourceCardConfig config);

    int updateDatasourceCardConfig(DatasourceCardConfig config);

    int deleteDatasourceCardConfigById(Long id);

    int deleteDatasourceCardConfigByIds(Long[] ids);
}
