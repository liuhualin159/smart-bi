package dev.lhl.query.service;

import java.util.List;
import java.util.Map;

/**
 * 图表推荐服务接口
 * 根据查询结果特征推荐合适的图表类型
 * 
 * @author smart-bi
 */
public interface IChartRecommendService
{
    /**
     * 推荐图表类型（仅列与数据，无用户问题/SQL 时走规则或 LLM 仅根据数据推断）
     *
     * @param columns 列名列表
     * @param data    数据行列表
     * @return 推荐的图表类型
     */
    ChartType recommendChartType(List<String> columns, List<Map<String, Object>> data);

    /**
     * 推荐图表类型（可选传入用户问题与 SQL，供 LLM 更好推断展示形式）
     *
     * @param columns  列名列表
     * @param data     数据行列表
     * @param question 用户自然语言问题（可为 null）
     * @param sql      执行的 SQL（可为 null）
     * @return 推荐的图表类型
     */
    ChartType recommendChartType(List<String> columns, List<Map<String, Object>> data, String question, String sql);

    /**
     * 推荐图表类型（含置信度与备选方案）
     *
     * @return ChartRecommendation 含 primary、confidence(0~1)、alternatives
     */
    ChartRecommendation recommendChartTypeWithConfidence(List<String> columns, List<Map<String, Object>> data, String question, String sql);

    /** 图表推荐结果 */
    record ChartRecommendation(ChartType primary, double confidence, java.util.List<ChartType> alternatives) {}
    
    /**
     * 图表类型枚举
     */
    enum ChartType
    {
        BAR("bar", "柱状图"),
        LINE("line", "折线图"),
        PIE("pie", "饼图"),
        GROUPED_BAR("groupedBar", "分组柱状图"),
        KPI("kpi", "指标卡"),
        TABLE("table", "表格"),
        HEATMAP("heatmap", "热力图"),
        FUNNEL("funnel", "漏斗图"),
        BOXPLOT("boxplot", "箱线图"),
        SCATTER("scatter", "散点图"),
        SANKEY("sankey", "桑基图");
        
        private final String code;
        private final String name;
        
        ChartType(String code, String name)
        {
            this.code = code;
            this.name = name;
        }
        
        public String getCode() { return code; }
        public String getName() { return name; }
    }
}
