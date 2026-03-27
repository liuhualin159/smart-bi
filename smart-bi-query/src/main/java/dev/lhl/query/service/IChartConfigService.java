package dev.lhl.query.service;

import java.util.List;
import java.util.Map;

/**
 * 图表配置生成服务接口
 * 根据图表类型和数据生成ECharts配置
 * 
 * @author smart-bi
 */
public interface IChartConfigService
{
    /**
     * 生成图表配置
     *
     * @param chartType 图表类型
     * @param columns 列名列表
     * @param data 数据行列表
     * @return ECharts配置对象（Map格式，可转换为JSON）
     */
    Map<String, Object> generateChartConfig(IChartRecommendService.ChartType chartType,
                                             List<String> columns,
                                             List<Map<String, Object>> data);

    /**
     * 生成图表配置（含列显示格式，用于 axisLabel.formatter 及正负值变色）
     *
     * @param columnDisplayFormats 列名 -> 显示格式（decimal|percent|currency|large），可为空
     */
    Map<String, Object> generateChartConfig(IChartRecommendService.ChartType chartType,
                                           List<String> columns,
                                           List<Map<String, Object>> data,
                                           Map<String, String> columnDisplayFormats);
}
