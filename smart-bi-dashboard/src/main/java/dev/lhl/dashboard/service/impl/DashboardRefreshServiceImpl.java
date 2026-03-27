package dev.lhl.dashboard.service.impl;

import dev.lhl.dashboard.domain.Dashboard;
import dev.lhl.dashboard.domain.DashboardCard;
import dev.lhl.dashboard.domain.ChartCard;
import dev.lhl.dashboard.mapper.DashboardCardMapper;
import dev.lhl.dashboard.service.IDashboardRefreshService;
import dev.lhl.dashboard.service.IDashboardService;
import dev.lhl.dashboard.service.IChartCardService;
import dev.lhl.query.service.IQueryExecutionService;
import dev.lhl.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 看板刷新服务实现
 * 负责刷新看板中的卡片数据
 * 
 * @author smart-bi
 */
@Service
public class DashboardRefreshServiceImpl implements IDashboardRefreshService
{
    private static final Logger log = LoggerFactory.getLogger(DashboardRefreshServiceImpl.class);
    
    @Autowired
    private IDashboardService dashboardService;
    
    @Autowired
    private IChartCardService chartCardService;
    
    @Autowired
    private DashboardCardMapper dashboardCardMapper;
    
    @Autowired(required = false)
    private IQueryExecutionService queryExecutionService;
    
    @Override
    public RefreshResult refreshDashboard(Long dashboardId)
    {
        try
        {
            log.info("开始刷新看板: dashboardId={}", dashboardId);
            
            // 1. 获取看板信息
            Dashboard dashboard = dashboardService.selectDashboardById(dashboardId);
            if (dashboard == null)
            {
                return RefreshResult.failure(0, "看板不存在");
            }
            
            // 2. 获取看板的所有卡片
            List<DashboardCard> dashboardCards = dashboardCardMapper.selectDashboardCardListByDashboardId(dashboardId);
            if (dashboardCards == null || dashboardCards.isEmpty())
            {
                log.info("看板没有卡片，无需刷新: dashboardId={}", dashboardId);
                return RefreshResult.success(0);
            }
            
            // 3. 刷新每个卡片
            int successCount = 0;
            int failCount = 0;
            Date now = new Date();
            
            for (DashboardCard dashboardCard : dashboardCards)
            {
                try
                {
                    // 获取卡片信息
                    ChartCard card = chartCardService.selectChartCardById(dashboardCard.getCardId());
                    if (card == null)
                    {
                        log.warn("卡片不存在，跳过刷新: cardId={}", dashboardCard.getCardId());
                        failCount++;
                        continue;
                    }
                    
                    // 刷新卡片数据
                    boolean refreshed = refreshCard(card.getId());
                    if (refreshed)
                    {
                        // 更新卡片最后刷新时间
                        card.setLastRefreshTime(now);
                        chartCardService.updateChartCard(card);
                        successCount++;
                        log.debug("卡片刷新成功: cardId={}", card.getId());
                    }
                    else
                    {
                        failCount++;
                        log.warn("卡片刷新失败: cardId={}", card.getId());
                    }
                }
                catch (Exception e)
                {
                    log.error("刷新卡片失败: cardId={}", dashboardCard.getCardId(), e);
                    failCount++;
                }
            }
            
            // 4. 返回刷新结果
            String message;
            if (failCount == 0)
            {
                message = String.format("看板刷新成功，共刷新 %d 个卡片", successCount);
            }
            else
            {
                message = String.format("看板刷新完成，成功 %d 个，失败 %d 个", successCount, failCount);
            }
            
            log.info("看板刷新完成: dashboardId={}, successCount={}, failCount={}", 
                dashboardId, successCount, failCount);
            
            return RefreshResult.partial(successCount, failCount, message);
        }
        catch (Exception e)
        {
            log.error("刷新看板失败: dashboardId={}", dashboardId, e);
            return RefreshResult.failure(0, "刷新看板失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean refreshCard(Long cardId)
    {
        try
        {
            log.debug("开始刷新卡片: cardId={}", cardId);
            
            // 1. 获取卡片信息
            ChartCard card = chartCardService.selectChartCardById(cardId);
            if (card == null)
            {
                log.warn("卡片不存在: cardId={}", cardId);
                return false;
            }
            
            // 2. 如果卡片有关联的SQL，重新执行查询
            if (StringUtils.isNotEmpty(card.getSql()))
            {
                if (queryExecutionService == null)
                {
                    log.warn("查询执行服务未配置，无法刷新卡片数据: cardId={}", cardId);
                    return false;
                }
                
                // 创建查询记录
                dev.lhl.query.domain.QueryRecord queryRecord = new dev.lhl.query.domain.QueryRecord();
                queryRecord.setExecutedSql(card.getSql());
                queryRecord.setUserId(card.getUserId());
                
                // 执行查询
                IQueryExecutionService.QueryResult queryResult = 
                    queryExecutionService.executeQuery(queryRecord, card.getUserId());
                
                if (queryResult.isSuccess() && queryResult.getData() != null)
                {
                    // 更新卡片配置（包含最新数据）
                    Map<String, Object> chartConfig = parseChartConfig(card.getChartConfig());
                    if (chartConfig != null)
                    {
                        chartConfig.put("data", queryResult.getData());
                        card.setChartConfig(com.alibaba.fastjson2.JSON.toJSONString(chartConfig));
                        chartCardService.updateChartCard(card);
                    }
                    
                    log.debug("卡片数据刷新成功: cardId={}, rowCount={}", cardId, queryResult.getRowCount());
                    return true;
                }
                else
                {
                    log.warn("卡片查询执行失败: cardId={}, error={}", cardId, queryResult.getErrorMessage());
                    return false;
                }
            }
            else
            {
                // 没有SQL的卡片，只更新刷新时间
                card.setLastRefreshTime(new Date());
                chartCardService.updateChartCard(card);
                log.debug("卡片无SQL，仅更新刷新时间: cardId={}", cardId);
                return true;
            }
        }
        catch (Exception e)
        {
            log.error("刷新卡片失败: cardId={}", cardId, e);
            return false;
        }
    }
    
    @Override
    public Map<String, Object> getChartCardData(Long cardId, Long userId)
    {
        if (cardId == null || userId == null)
        {
            return null;
        }
        ChartCard card = chartCardService.selectChartCardById(cardId);
        if (card == null || StringUtils.isEmpty(card.getSql()))
        {
            return null;
        }
        if (queryExecutionService == null)
        {
            log.warn("查询执行服务未配置: cardId={}", cardId);
            return null;
        }
        dev.lhl.query.domain.QueryRecord queryRecord = new dev.lhl.query.domain.QueryRecord();
        queryRecord.setExecutedSql(card.getSql());
        queryRecord.setUserId(userId);
        IQueryExecutionService.QueryResult queryResult = queryExecutionService.executeQuery(queryRecord, userId);
        if (!queryResult.isSuccess())
        {
            return null;
        }
        List<Map<String, Object>> data = queryResult.getData();
        List<String> columns = data != null && !data.isEmpty()
            ? new ArrayList<>(data.get(0).keySet())
            : new ArrayList<>();
        List<Map<String, Object>> dataJsonFriendly = data != null && !data.isEmpty()
            ? toJsonFriendlyRows(data)
            : new ArrayList<>();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", card.getChartType() != null ? card.getChartType() : "table");
        result.put("columns", columns);
        result.put("data", dataJsonFriendly);
        return result;
    }

    private static List<Map<String, Object>> toJsonFriendlyRows(List<Map<String, Object>> data)
    {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : data)
        {
            Map<String, Object> clean = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : row.entrySet())
            {
                Object v = e.getValue();
                clean.put(e.getKey(), toJsonFriendlyValue(v));
            }
            out.add(clean);
        }
        return out;
    }

    private static Object toJsonFriendlyValue(Object v)
    {
        if (v == null) return null;
        if (v instanceof Number || v instanceof Boolean || v instanceof String) return v;
        if (v instanceof java.util.Date) return ((java.util.Date) v).getTime();
        if (v instanceof java.time.temporal.TemporalAccessor) return v.toString();
        if (v instanceof byte[]) return new String((byte[]) v, java.nio.charset.StandardCharsets.UTF_8);
        return String.valueOf(v);
    }

    /**
     * 解析图表配置JSON
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseChartConfig(String chartConfigJson)
    {
        if (StringUtils.isEmpty(chartConfigJson))
        {
            return null;
        }
        
        try
        {
            return com.alibaba.fastjson2.JSON.parseObject(chartConfigJson, Map.class);
        }
        catch (Exception e)
        {
            log.warn("解析图表配置失败: chartConfig={}", chartConfigJson, e);
            return null;
        }
    }
}
