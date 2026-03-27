package dev.lhl.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

/**
 * 图表推荐工具类
 * 根据查询结果特征自动推荐合适的图表类型
 * 
 * @author smart-bi
 */
public class ChartRecommendUtils
{
    private static final Logger log = LoggerFactory.getLogger(ChartRecommendUtils.class);
    
    /**
     * 图表类型枚举
     */
    public enum ChartType
    {
        BAR("bar", "柱状图"),
        LINE("line", "折线图"),
        PIE("pie", "饼图"),
        GROUPED_BAR("groupedBar", "分组柱状图"),
        KPI("kpi", "指标卡"),
        TABLE("table", "表格");
        
        private final String code;
        private final String name;
        
        ChartType(String code, String name)
        {
            this.code = code;
            this.name = name;
        }
        
        public String getCode()
        {
            return code;
        }
        
        public String getName()
        {
            return name;
        }
    }
    
    /**
     * 根据查询结果推荐图表类型
     * 
     * 规则：
     * - 1维度+1度量 → 饼图（枚举值数量≤10）或柱状图（枚举值数量>10）
     * - 时间维度+1度量 → 折线图
     * - 2维度+1度量 → 分组柱状图
     * - 无维度+多度量 → 指标卡/KPI卡片
     * 
     * @param columns 列名列表
     * @param rows 数据行列表
     * @param dimensionCount 维度数量
     * @param measureCount 度量数量
     * @param hasTimeDimension 是否包含时间维度
     * @param enumValueCount 枚举值数量（仅当dimensionCount=1时有效）
     * @return 推荐的图表类型
     */
    public static ChartType recommendChartType(
        List<String> columns,
        List<Map<String, Object>> rows,
        int dimensionCount,
        int measureCount,
        boolean hasTimeDimension,
        int enumValueCount)
    {
        if (rows == null || rows.isEmpty())
        {
            log.warn("数据为空，无法推荐图表类型");
            return ChartType.KPI;
        }
        
        // 多维度、少度量且无时间维度的明细型结果，更适合用表格展示
        if (!hasTimeDimension && dimensionCount >= 3 && measureCount <= 1)
        {
            log.debug("推荐表格: 多维度明细数据，dimensionCount={}, measureCount={}", dimensionCount, measureCount);
            return ChartType.TABLE;
        }
        
        // 时间维度+1度量 → 折线图
        if (hasTimeDimension && dimensionCount == 1 && measureCount == 1)
        {
            log.debug("推荐折线图: 时间维度+1度量");
            return ChartType.LINE;
        }
        
        // 1维度+1度量：统计类（如「每个用户的订单总金额」）默认柱状图；仅当枚举值≤2时用饼图
        if (dimensionCount == 1 && measureCount == 1)
        {
            if (enumValueCount <= 2)
            {
                log.debug("推荐饼图: 1维度+1度量，枚举值数量={}", enumValueCount);
                return ChartType.PIE;
            }
            log.debug("推荐柱状图: 1维度+1度量，枚举值数量={}", enumValueCount);
            return ChartType.BAR;
        }
        
        // 2维度+1度量 → 分组柱状图
        if (dimensionCount == 2 && measureCount == 1)
        {
            log.debug("推荐分组柱状图: 2维度+1度量");
            return ChartType.GROUPED_BAR;
        }
        
        // 无维度+纯度量（如 SELECT COUNT(*) AS 项目总数）→ 指标卡
        if (dimensionCount == 0 && measureCount >= 1)
        {
            log.debug("推荐指标卡: 无维度+{}度量", measureCount);
            return ChartType.KPI;
        }
        
        // 默认返回柱状图
        log.debug("默认推荐柱状图");
        return ChartType.BAR;
    }
    
    /**
     * 计算枚举值数量
     * 
     * @param rows 数据行
     * @param dimensionColumn 维度列名
     * @return 枚举值数量
     */
    public static int countEnumValues(List<Map<String, Object>> rows, String dimensionColumn)
    {
        if (rows == null || rows.isEmpty() || StringUtils.isEmpty(dimensionColumn))
        {
            return 0;
        }
        
        return (int) rows.stream()
            .map(row -> row.get(dimensionColumn))
            .distinct()
            .count();
    }
    
    /**
     * 判断是否包含时间维度
     * 
     * @param columns 列名列表
     * @return true表示包含时间维度
     */
    public static boolean hasTimeDimension(List<String> columns)
    {
        if (columns == null || columns.isEmpty())
        {
            return false;
        }
        
        // 时间维度常见列名
        String[] timeKeywords = {"date", "time", "year", "month", "day", "week", "季度", "日期", "时间", "年", "月", "日"};
        
        for (String column : columns)
        {
            String lowerColumn = column.toLowerCase();
            for (String keyword : timeKeywords)
            {
                if (lowerColumn.contains(keyword.toLowerCase()))
                {
                    return true;
                }
            }
        }
        
        return false;
    }
}
