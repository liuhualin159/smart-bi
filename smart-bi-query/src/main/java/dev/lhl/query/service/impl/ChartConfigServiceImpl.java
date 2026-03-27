package dev.lhl.query.service.impl;

import dev.lhl.query.service.IChartConfigService;
import dev.lhl.query.service.IChartRecommendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 图表配置生成服务实现
 * 根据图表类型和数据生成ECharts配置
 * 
 * @author smart-bi
 */
@Service
public class ChartConfigServiceImpl implements IChartConfigService
{
    private static final Logger log = LoggerFactory.getLogger(ChartConfigServiceImpl.class);
    
    private Map<String, String> columnDisplayFormats;

    @Override
    public Map<String, Object> generateChartConfig(IChartRecommendService.ChartType chartType,
                                                    List<String> columns,
                                                    List<Map<String, Object>> data)
    {
        return generateChartConfig(chartType, columns, data, null);
    }

    @Override
    public Map<String, Object> generateChartConfig(IChartRecommendService.ChartType chartType,
                                                    List<String> columns,
                                                    List<Map<String, Object>> data,
                                                    Map<String, String> columnDisplayFormats)
    {
        this.columnDisplayFormats = columnDisplayFormats;
        try
        {
            log.debug("生成图表配置: chartType={}, columns={}, dataSize={}",
                chartType, columns, data != null ? data.size() : 0);

            if (data == null || data.isEmpty())
            {
                return generateEmptyChartConfig(chartType);
            }

            switch (chartType)
            {
                case BAR:
                    return generateBarChartConfig(columns, data);
                case LINE:
                    return generateLineChartConfig(columns, data);
                case PIE:
                    return generatePieChartConfig(columns, data);
                case GROUPED_BAR:
                    return generateGroupedBarChartConfig(columns, data);
                case KPI:
                    return generateKpiChartConfig(columns, data);
                case HEATMAP:
                    return generateHeatmapChartConfig(columns, data);
                case FUNNEL:
                    return generateFunnelChartConfig(columns, data);
                case BOXPLOT:
                    return generateBoxplotChartConfig(columns, data);
                case SCATTER:
                    return generateScatterChartConfig(columns, data);
                case SANKEY:
                    return generateSankeyChartConfig(columns, data);
                case TABLE:
                default:
                    return generateTableConfig(columns, data);
            }
        }
        catch (Exception e)
        {
            log.error("生成图表配置失败: chartType={}", chartType, e);
            return generateErrorChartConfig("图表配置生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成柱状图配置
     */
    private Map<String, Object> generateBarChartConfig(List<String> columns, List<Map<String, Object>> data)
    {
        Map<String, Object> config = new HashMap<>();
        
        // 识别维度列和度量列
        String dimensionColumn = findFirstDimensionColumn(columns, data);
        String measureColumn = findFirstMeasureColumn(columns, data);
        
        // 纯度量结果（如 COUNT/SUM 无 GROUP BY）→ 降级为指标卡
        if (dimensionColumn == null && measureColumn != null)
        {
            return generateKpiChartConfig(columns, data);
        }
        if (dimensionColumn == null || measureColumn == null)
        {
            return generateErrorChartConfig("无法识别维度或度量列");
        }
        
        // 提取数据
        List<String> categories = new ArrayList<>();
        List<Number> values = new ArrayList<>();
        
        for (Map<String, Object> row : data)
        {
            Object dimValue = row.get(dimensionColumn);
            Object measureValue = row.get(measureColumn);
            
            if (dimValue != null && measureValue != null)
            {
                categories.add(String.valueOf(dimValue));
                values.add(convertToNumber(measureValue));
            }
        }
        
        // 构建ECharts配置（series 必须带 name，否则 legend 会报 undefined series）
        String seriesName = measureColumn != null ? measureColumn : "数值";
        config.put("title", createTitle("柱状图"));
        config.put("tooltip", createTooltip());
        config.put("xAxis", Map.of("type", "category", "data", categories));
        Map<String, Object> yAxis = new HashMap<>(Map.of("type", "value"));
        applyAxisFormatter(yAxis, measureColumn, values);
        config.put("yAxis", yAxis);
        boolean hasNegative = values.stream().anyMatch(v -> v != null && v.doubleValue() < 0);
        Map<String, Object> seriesItem = new HashMap<>();
        seriesItem.put("type", "bar");
        seriesItem.put("name", seriesName);
        seriesItem.put("data", values);
        seriesItem.put("itemStyle", hasNegative ? colorBySign() : Map.of("color", "#409EFF"));
        config.put("series", List.of(seriesItem));

        return config;
    }

    /**
     * 生成折线图配置。当有「时间维度+主体维度+度量」时，生成多系列折线图（每个主体一条线）。
     */
    private Map<String, Object> generateLineChartConfig(List<String> columns, List<Map<String, Object>> data)
    {
        Map<String, Object> config = new HashMap<>();
        String measureColumn = findFirstMeasureColumn(columns, data);
        if (measureColumn == null)
        {
            return generateErrorChartConfig("无法识别度量列");
        }
        
        List<String> dimensionColumns = findDimensionColumns(columns, data);
        if (dimensionColumns.isEmpty())
        {
            return generateErrorChartConfig("无法识别维度列");
        }
        
        // 尝试多系列：2 维度 + 1 度量，且有一列为时间/日期类
        String timeColumn = findTimeDimensionColumn(dimensionColumns, data);
        if (timeColumn != null && dimensionColumns.size() >= 2)
        {
            String categoryColumn = dimensionColumns.stream()
                .filter(c -> !c.equals(timeColumn))
                .findFirst()
                .orElse(null);
            if (categoryColumn != null)
            {
                Map<String, Object> multiLine = generateMultiSeriesLineConfig(
                    timeColumn, categoryColumn, measureColumn, data);
                if (multiLine != null)
                {
                    return multiLine;
                }
            }
        }
        
        // 降级：单维度 + 度量
        String dimensionColumn = dimensionColumns.get(0);
        List<String> categories = new ArrayList<>();
        List<Number> values = new ArrayList<>();
        for (Map<String, Object> row : data)
        {
            Object dimValue = row.get(dimensionColumn);
            Object measureValue = row.get(measureColumn);
            if (dimValue != null && measureValue != null)
            {
                categories.add(String.valueOf(dimValue));
                values.add(convertToNumber(measureValue));
            }
        }
        
        String seriesName = measureColumn != null ? measureColumn : "数值";
        config.put("title", createTitle("折线图"));
        config.put("tooltip", createTooltip());
        config.put("xAxis", Map.of("type", "category", "data", categories));
        Map<String, Object> yAxis = new HashMap<>(Map.of("type", "value"));
        applyAxisFormatter(yAxis, measureColumn, values);
        config.put("yAxis", yAxis);
        boolean hasNegative = values.stream().anyMatch(v -> v != null && v.doubleValue() < 0);
        Map<String, Object> lineSeries = new HashMap<>();
        lineSeries.put("type", "line");
        lineSeries.put("name", seriesName);
        lineSeries.put("data", values);
        lineSeries.put("smooth", true);
        lineSeries.put("itemStyle", hasNegative ? colorBySign() : Map.of("color", "#409EFF"));
        config.put("series", List.of(lineSeries));
        return config;
    }
    
    /** 多系列折线图：X=时间，每条线=一个主体 */
    private Map<String, Object> generateMultiSeriesLineConfig(
        String timeColumn, String categoryColumn, String measureColumn, List<Map<String, Object>> data)
    {
        Set<String> dates = new LinkedHashSet<>();
        Map<String, Map<String, Number>> seriesData = new LinkedHashMap<>();
        for (Map<String, Object> row : data)
        {
            Object timeVal = row.get(timeColumn);
            Object catVal = row.get(categoryColumn);
            Object measureVal = row.get(measureColumn);
            if (timeVal == null || catVal == null || measureVal == null) continue;
            String dateStr = String.valueOf(timeVal).trim();
            String catStr = String.valueOf(catVal);
            dates.add(dateStr);
            seriesData.computeIfAbsent(catStr, k -> new LinkedHashMap<>()).put(dateStr, convertToNumber(measureVal));
        }
        List<String> xData = new ArrayList<>(dates);
        xData.sort(String::compareTo);
        if (xData.isEmpty() || seriesData.isEmpty()) return null;
        
        String[] colors = {"#409EFF", "#67C23A", "#E6A23C", "#F56C6C", "#909399"};
        List<Map<String, Object>> series = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Map<String, Number>> e : seriesData.entrySet())
        {
            List<Number> vals = new ArrayList<>();
            for (String d : xData)
            {
                Number v = e.getValue().get(d);
                vals.add(v != null ? v : null);
            }
            series.add(Map.of(
                "type", "line",
                "name", e.getKey(),
                "data", vals,
                "smooth", true,
                "connectNulls", true,
                "itemStyle", Map.of("color", colors[i % colors.length])
            ));
            i++;
        }
        
        Map<String, Object> config = new HashMap<>();
        config.put("title", createTitle("折线图"));
        config.put("tooltip", createTooltip());
        config.put("legend", Map.of("data", new ArrayList<>(seriesData.keySet())));
        config.put("xAxis", Map.of("type", "category", "data", xData));
        config.put("yAxis", Map.of("type", "value"));
        config.put("series", series);
        return config;
    }
    
    private List<String> findDimensionColumns(List<String> columns, List<Map<String, Object>> data)
    {
        List<String> result = new ArrayList<>();
        for (String col : columns)
        {
            if (!isMeasureColumn(col, data))
                result.add(col);
        }
        return result;
    }
    
    private static final java.util.regex.Pattern DATE_PATTERN =
        java.util.regex.Pattern.compile("^\\d{4}-\\d{2}-\\d{2}(\\s|T)?");
    
    private String findTimeDimensionColumn(List<String> dimensionColumns, List<Map<String, Object>> data)
    {
        for (String col : dimensionColumns)
        {
            if (col == null) continue;
            String lower = col.toLowerCase();
            if (lower.contains("date") || lower.contains("time") || lower.contains("时间") || lower.contains("日期"))
                return col;
        }
        if (data != null && !data.isEmpty())
        {
            for (String col : dimensionColumns)
            {
                Object v = data.get(0).get(col);
                if (v != null && DATE_PATTERN.matcher(String.valueOf(v)).find())
                    return col;
            }
        }
        return null;
    }
    
    /**
     * 生成饼图配置
     */
    private Map<String, Object> generatePieChartConfig(List<String> columns, List<Map<String, Object>> data)
    {
        Map<String, Object> config = new HashMap<>();
        
        String dimensionColumn = findFirstDimensionColumn(columns, data);
        String measureColumn = findFirstMeasureColumn(columns, data);
        
        if (dimensionColumn == null && measureColumn != null)
        {
            return generateKpiChartConfig(columns, data);
        }
        if (dimensionColumn == null || measureColumn == null)
        {
            return generateErrorChartConfig("无法识别维度或度量列");
        }
        
        List<Map<String, Object>> pieData = new ArrayList<>();
        
        for (Map<String, Object> row : data)
        {
            Object dimValue = row.get(dimensionColumn);
            Object measureValue = row.get(measureColumn);
            
            if (dimValue != null && measureValue != null)
            {
                pieData.add(Map.of(
                    "name", String.valueOf(dimValue),
                    "value", convertToNumber(measureValue)
                ));
            }
        }
        
        String seriesName = measureColumn != null ? measureColumn : "数值";
        config.put("title", createTitle("饼图"));
        config.put("tooltip", createTooltip());
        config.put("series", List.of(
            Map.of(
                "type", "pie",
                "name", seriesName,
                "radius", "60%",
                "data", pieData,
                "emphasis", Map.of(
                    "itemStyle", Map.of(
                        "shadowBlur", 10,
                        "shadowOffsetX", 0,
                        "shadowColor", "rgba(0, 0, 0, 0.5)"
                    )
                )
            )
        ));
        
        return config;
    }
    
    /**
     * 生成分组柱状图配置
     */
    private Map<String, Object> generateGroupedBarChartConfig(List<String> columns, List<Map<String, Object>> data)
    {
        // 分组柱状图实现：当数据包含多个维度时，使用第一个维度作为分组
        // 注意：完整实现需要识别多个维度并正确分组，当前实现降级为普通柱状图
        return generateBarChartConfig(columns, data);
    }
    
    /**
     * 生成指标卡配置
     */
    private Map<String, Object> generateKpiChartConfig(List<String> columns, List<Map<String, Object>> data)
    {
        Map<String, Object> config = new HashMap<>();
        config.put("type", "kpi");
        config.put("data", data);
        config.put("columns", columns);
        return config;
    }
    
    /** 生成热力图配置：需 2 维度 + 1 度量，维度作为 x/y，度量作为色阶 */
    private Map<String, Object> generateHeatmapChartConfig(List<String> columns, List<Map<String, Object>> data)
    {
        List<String> dims = findDimensionColumns(columns, data);
        String measure = findFirstMeasureColumn(columns, data);
        if (dims.size() < 2 || measure == null) return generateErrorChartConfig("热力图需要至少 2 个维度列和 1 个度量列");
        String xDim = dims.get(0);
        String yDim = dims.get(1);
        Set<String> xVals = new LinkedHashSet<>();
        Set<String> yVals = new LinkedHashSet<>();
        List<List<Object>> heatData = new ArrayList<>();
        for (Map<String, Object> row : data)
        {
            Object xv = row.get(xDim);
            Object yv = row.get(yDim);
            Object mv = row.get(measure);
            if (xv == null || yv == null || mv == null) continue;
            String xs = String.valueOf(xv);
            String ys = String.valueOf(yv);
            xVals.add(xs);
            yVals.add(ys);
            heatData.add(List.of(xs, ys, convertToNumber(mv)));
        }
        List<String> xList = new ArrayList<>(xVals);
        List<String> yList = new ArrayList<>(yVals);
        Map<String, Object> config = new HashMap<>();
        config.put("title", createTitle("热力图"));
        config.put("tooltip", Map.of("position", "top"));
        config.put("grid", Map.of("height", "50%", "top", "10%"));
        config.put("xAxis", Map.of("type", "category", "data", xList, "splitArea", Map.of("show", true)));
        config.put("yAxis", Map.of("type", "category", "data", yList, "splitArea", Map.of("show", true)));
        config.put("visualMap", Map.of("min", 0, "max", 100, "calculable", true, "orient", "horizontal", "left", "center", "bottom", "5%"));
        config.put("series", List.of(Map.of("type", "heatmap", "data", heatData, "label", Map.of("show", false))));
        return config;
    }

    /** 生成漏斗图配置：维度 + 度量，按度量降序 */
    private Map<String, Object> generateFunnelChartConfig(List<String> columns, List<Map<String, Object>> data)
    {
        String dim = findFirstDimensionColumn(columns, data);
        String measure = findFirstMeasureColumn(columns, data);
        if (dim == null || measure == null) return generateErrorChartConfig("漏斗图需要维度列和度量列");
        List<Map<String, Object>> funnelData = new ArrayList<>();
        for (Map<String, Object> row : data)
        {
            Object dv = row.get(dim);
            Object mv = row.get(measure);
            if (dv != null && mv != null)
                funnelData.add(Map.of("name", String.valueOf(dv), "value", convertToNumber(mv)));
        }
        funnelData.sort((a, b) -> Double.compare(((Number) b.get("value")).doubleValue(), ((Number) a.get("value")).doubleValue()));
        Map<String, Object> config = new HashMap<>();
        config.put("title", createTitle("漏斗图"));
        config.put("tooltip", Map.of("trigger", "item"));
        config.put("series", List.of(Map.of("type", "funnel", "data", funnelData, "gap", 2)));
        return config;
    }

    /** 生成箱线图配置：需数值列，支持多列对比 */
    private Map<String, Object> generateBoxplotChartConfig(List<String> columns, List<Map<String, Object>> data)
    {
        List<String> measures = new ArrayList<>();
        for (String col : columns)
            if (isMeasureColumn(col, data)) measures.add(col);
        if (measures.isEmpty()) return generateErrorChartConfig("箱线图需要数值列");
        List<String> categories = new ArrayList<>();
        List<List<Number>> boxData = new ArrayList<>();
        for (int i = 0; i < data.size(); i++)
        {
            Map<String, Object> row = data.get(i);
            categories.add(String.valueOf(i + 1));
            List<Number> vals = new ArrayList<>();
            for (String m : measures)
            {
                Object v = row.get(m);
                if (v != null) vals.add(convertToNumber(v));
            }
            boxData.add(vals);
        }
        List<Object> seriesData = new ArrayList<>();
        for (List<Number> vals : boxData)
        {
            if (vals.isEmpty()) seriesData.add(List.of(0, 0, 0, 0, 0));
            else
            {
                double[] arr = vals.stream().mapToDouble(Number::doubleValue).sorted().toArray();
                double min = arr[0], q1 = arr[arr.length / 4], median = arr[arr.length / 2], q3 = arr[arr.length * 3 / 4], max = arr[arr.length - 1];
                seriesData.add(List.of(min, q1, median, q3, max));
            }
        }
        Map<String, Object> config = new HashMap<>();
        config.put("title", createTitle("箱线图"));
        config.put("tooltip", Map.of("trigger", "item"));
        config.put("xAxis", Map.of("type", "category", "data", categories));
        config.put("yAxis", Map.of("type", "value"));
        config.put("series", List.of(Map.of("type", "boxplot", "data", seriesData)));
        return config;
    }

    /** 生成散点图配置：2 数值列，或 1 维度 + 2 数值 */
    private Map<String, Object> generateScatterChartConfig(List<String> columns, List<Map<String, Object>> data)
    {
        List<String> measures = new ArrayList<>();
        String dim = findFirstDimensionColumn(columns, data);
        for (String col : columns)
            if (isMeasureColumn(col, data)) measures.add(col);
        if (measures.size() < 2) return generateErrorChartConfig("散点图需要至少 2 个数值列");
        String xCol = measures.get(0);
        String yCol = measures.get(1);
        List<List<Number>> scatterData = new ArrayList<>();
        for (Map<String, Object> row : data)
        {
            Object xv = row.get(xCol);
            Object yv = row.get(yCol);
            if (xv != null && yv != null)
                scatterData.add(List.of(convertToNumber(xv), convertToNumber(yv)));
        }
        Map<String, Object> config = new HashMap<>();
        config.put("title", createTitle("散点图"));
        config.put("tooltip", Map.of("trigger", "item"));
        config.put("xAxis", Map.of("type", "value", "name", xCol));
        config.put("yAxis", Map.of("type", "value", "name", yCol));
        config.put("series", List.of(Map.of("type", "scatter", "data", scatterData, "symbolSize", 8)));
        return config;
    }

    /** 生成桑基图配置：需 source、target、value 或 2 维度 + 1 度量 */
    private Map<String, Object> generateSankeyChartConfig(List<String> columns, List<Map<String, Object>> data)
    {
        if (columns.size() < 2)
            return generateErrorChartConfig("桑基图需要至少 2 列（源、目标、值）");
        List<String> dims = findDimensionColumns(columns, data);
        String measure = findFirstMeasureColumn(columns, data);
        if (dims.size() < 2 || measure == null) return generateErrorChartConfig("桑基图需要至少 2 个维度列和 1 个度量列");
        Set<String> nodes = new LinkedHashSet<>();
        List<Map<String, Object>> links = new ArrayList<>();
        for (Map<String, Object> row : data)
        {
            String s = String.valueOf(row.get(dims.get(0)));
            String t = String.valueOf(row.get(dims.get(1)));
            Object v = row.get(measure);
            if (s == null || "null".equals(s) || t == null || "null".equals(t) || v == null) continue;
            nodes.add(s);
            nodes.add(t);
            links.add(Map.of("source", s, "target", t, "value", convertToNumber(v)));
        }
        List<Map<String, Object>> nodeList = new ArrayList<>();
        for (String n : nodes) nodeList.add(Map.of("name", n));
        Map<String, Object> config = new HashMap<>();
        config.put("title", createTitle("桑基图"));
        config.put("tooltip", Map.of("trigger", "item"));
        config.put("series", List.of(Map.of("type", "sankey", "data", nodeList, "links", links, "lineStyle", Map.of("opacity", 0.2))));
        return config;
    }

    /**
     * 生成表格配置
     */
    private Map<String, Object> generateTableConfig(List<String> columns, List<Map<String, Object>> data)
    {
        Map<String, Object> config = new HashMap<>();
        config.put("type", "table");
        config.put("columns", columns);
        config.put("data", data);
        return config;
    }
    
    /**
     * 生成空数据图表配置
     */
    private Map<String, Object> generateEmptyChartConfig(IChartRecommendService.ChartType chartType)
    {
        Map<String, Object> config = new HashMap<>();
        config.put("type", chartType.getCode());
        config.put("title", createTitle("暂无数据"));
        config.put("data", Collections.emptyList());
        return config;
    }
    
    /**
     * 生成错误图表配置
     */
    private Map<String, Object> generateErrorChartConfig(String errorMessage)
    {
        Map<String, Object> config = new HashMap<>();
        config.put("error", true);
        config.put("errorMessage", errorMessage);
        config.put("title", createTitle("图表渲染失败"));
        return config;
    }
    
    /**
     * 按列名判断是否更像维度（用于分组/分类，如 id、*_id、name、code、type）
     * 避免把 customer_id 等判成度量导致推荐 KPI 或柱状图无法识别维度
     */
    private static boolean isLikelyDimensionColumn(String columnName)
    {
        if (columnName == null || columnName.isEmpty()) return false;
        String lower = columnName.toLowerCase();
        return lower.endsWith("_id") || "id".equals(lower)
            || lower.contains("name") || lower.contains("code") || lower.contains("type")
            || lower.contains("category") || lower.contains("level") || lower.contains("status");
    }

    /**
     * 按列名判断是否更像度量（聚合结果，如 total、sum、amount、count、avg）
     */
    private static boolean isLikelyMeasureColumn(String columnName)
    {
        if (columnName == null || columnName.isEmpty()) return false;
        String lower = columnName.toLowerCase();
        return lower.contains("total") || lower.contains("sum_") || lower.contains("amount")
            || lower.contains("count") || lower.contains("avg") || lower.contains("average")
            || lower.contains("num_") || lower.contains("_count") || lower.contains("_sum");
    }

    /**
     * 查找第一个维度列（优先按列名识别 *_id、id、name 等，否则取第一个非数值列）
     */
    private String findFirstDimensionColumn(List<String> columns, List<Map<String, Object>> data)
    {
        for (String column : columns)
        {
            if (isLikelyDimensionColumn(column))
            {
                return column;
            }
        }
        for (String column : columns)
        {
            if (!isMeasureColumn(column, data))
            {
                return column;
            }
        }
        return null;
    }

    /**
     * 查找第一个度量列（优先按列名识别 total、amount、count 等且为数值，否则取第一个数值列）
     */
    private String findFirstMeasureColumn(List<String> columns, List<Map<String, Object>> data)
    {
        for (String column : columns)
        {
            if (isLikelyMeasureColumn(column) && isMeasureColumn(column, data))
            {
                return column;
            }
        }
        for (String column : columns)
        {
            if (isMeasureColumn(column, data))
            {
                return column;
            }
        }
        return null;
    }

    /**
     * 判断是否为度量列（数值类型）
     */
    private boolean isMeasureColumn(String column, List<Map<String, Object>> data)
    {
        if (data == null || data.isEmpty())
        {
            return false;
        }
        Object firstValue = data.get(0).get(column);
        if (firstValue == null)
        {
            return false;
        }
        if (firstValue instanceof Number)
        {
            return true;
        }
        // JDBC 可能返回字符串形式的数字（如 "1044"）
        if (firstValue instanceof String)
        {
            try
            {
                String s = ((String) firstValue).trim();
                if (s.isEmpty()) return false;
                Double.parseDouble(s.replace(",", ""));
                return true;
            }
            catch (NumberFormatException e)
            {
                return false;
            }
        }
        return false;
    }
    
    /**
     * 转换为数字
     */
    private Number convertToNumber(Object value)
    {
        if (value instanceof Number)
        {
            return (Number) value;
        }
        
        try
        {
            if (value instanceof String)
            {
                String str = (String) value;
                if (str.contains("."))
                {
                    return Double.parseDouble(str);
                }
                else
                {
                    return Long.parseLong(str);
                }
            }
        }
        catch (Exception e)
        {
            log.warn("转换数字失败: value={}", value, e);
        }
        
        return 0;
    }
    
    /**
     * 创建标题配置
     */
    private Map<String, Object> createTitle(String text)
    {
        return Map.of(
            "text", text,
            "left", "center",
            "textStyle", Map.of("fontSize", 16)
        );
    }
    
    private void applyAxisFormatter(Map<String, Object> yAxis, String measureColumn, List<Number> values) {
        if (columnDisplayFormats == null || measureColumn == null) return;
        String fmt = columnDisplayFormats.get(measureColumn);
        if (fmt == null || fmt.trim().isEmpty()) return;
        yAxis.put("axisLabel", Map.of("_formatterType", fmt.trim().toLowerCase()));
    }

    /** 正值绿负值红：前端需根据 _colorBySign 标记注入实际函数 */
    private Map<String, Object> colorBySign() {
        return Map.of("_colorBySign", true, "color", "#67C23A");
    }

    /**
     * 创建提示框配置
     */
    private Map<String, Object> createTooltip()
    {
        return Map.of(
            "trigger", "axis",
            "axisPointer", Map.of("type", "shadow")
        );
    }
}
