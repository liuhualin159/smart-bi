package dev.lhl.dashboard.service;

import dev.lhl.dashboard.domain.DatasourceCardConfig;
import java.util.List;
import java.util.Map;

/**
 * 数据源卡片配置Service接口
 *
 * @author smart-bi
 */
public interface IDatasourceCardConfigService
{
    DatasourceCardConfig selectDatasourceCardConfigById(Long id);
    DatasourceCardConfig selectByDashboardCardId(Long dashboardCardId);
    List<DatasourceCardConfig> selectDatasourceCardConfigList(DatasourceCardConfig config);
    int insertDatasourceCardConfig(DatasourceCardConfig config);
    int updateDatasourceCardConfig(DatasourceCardConfig config);
    int deleteDatasourceCardConfigById(Long id);
    int deleteDatasourceCardConfigByIds(Long[] ids);
    Map<String, Object> executeQuery(Long configId);
    Map<String, Object> previewQuery(DatasourceCardConfig config);
}
