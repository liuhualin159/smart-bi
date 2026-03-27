package dev.lhl.web.controller.dashboard;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import dev.lhl.common.annotation.Log;
import dev.lhl.common.core.controller.BaseController;
import dev.lhl.common.core.domain.AjaxResult;
import dev.lhl.common.core.page.TableDataInfo;
import dev.lhl.common.enums.BusinessType;
import dev.lhl.dashboard.domain.Dashboard;
import dev.lhl.dashboard.domain.ChartCard;
import dev.lhl.dashboard.domain.DashboardCard;
import dev.lhl.dashboard.domain.DatasourceCardConfig;
import dev.lhl.dashboard.domain.report.ReportGenerateProgressCallback;
import dev.lhl.dashboard.domain.report.ReportGenerateRequest;
import dev.lhl.dashboard.domain.report.ReportGenerateResult;
import dev.lhl.dashboard.service.IDashboardReportGenerateService;
import dev.lhl.dashboard.service.IDashboardService;
import dev.lhl.common.utils.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import dev.lhl.dashboard.service.IChartCardService;
import dev.lhl.dashboard.service.IDashboardRefreshService;
import dev.lhl.dashboard.service.IDatasourceCardConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 看板Controller
 * 
 * @author smart-bi
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    
    @Autowired
    private IDashboardService dashboardService;

    @Autowired
    private IChartCardService chartCardService;

    @PreAuthorize("@ss.hasPermi('dashboard:list')")
    @GetMapping("/list")
    public TableDataInfo list(Dashboard dashboard)
    {
        startPage();
        List<Dashboard> list = dashboardService.selectDashboardList(dashboard);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('dashboard:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        Dashboard dashboard = dashboardService.selectDashboardById(id);
        if (dashboard == null)
        {
            return error("看板不存在");
        }
        
        // 获取看板的卡片列表
        List<DashboardCard> dashboardCards = dashboardCardMapper.selectDashboardCardListByDashboardId(id);
        
        // 构建响应数据
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("dashboard", dashboard);
        result.put("cards", dashboardCards);
        
        return success(result);
    }
    
    /**
     * 获取看板的卡片列表
     */
    @PreAuthorize("@ss.hasPermi('dashboard:card:list')")
    @GetMapping("/{id}/cards")
    public AjaxResult getDashboardCards(@PathVariable("id") Long id)
    {
        try
        {
            List<DashboardCard> dashboardCards = dashboardCardMapper.selectDashboardCardListByDashboardId(id);
            
            List<java.util.Map<String, Object>> cardList = new java.util.ArrayList<>();
            for (DashboardCard dashboardCard : dashboardCards)
            {
                java.util.Map<String, Object> cardInfo = new java.util.HashMap<>();
                cardInfo.put("dashboardCardId", dashboardCard.getId());
                cardInfo.put("componentType", dashboardCard.getComponentType());
                cardInfo.put("styleConfig", dashboardCard.getStyleConfig());
                cardInfo.put("parentId", dashboardCard.getParentId());
                cardInfo.put("decorationType", dashboardCard.getDecorationType());
                cardInfo.put("cardName", dashboardCard.getCardName());
                cardInfo.put("positionX", dashboardCard.getPositionX());
                cardInfo.put("positionY", dashboardCard.getPositionY());
                cardInfo.put("width", dashboardCard.getWidth());
                cardInfo.put("height", dashboardCard.getHeight());
                cardInfo.put("sortOrder", dashboardCard.getSortOrder());

                String componentType = dashboardCard.getComponentType();
                if (("chart".equals(componentType) || componentType == null) && dashboardCard.getCardId() != null)
                {
                    ChartCard card = chartCardService.selectChartCardById(dashboardCard.getCardId());
                    if (card != null)
                    {
                        cardInfo.put("cardId", card.getId());
                        cardInfo.put("chartType", card.getChartType());
                        cardInfo.put("chartConfig", card.getChartConfig());
                        if (dashboardCard.getCardName() == null)
                        {
                            cardInfo.put("cardName", card.getName());
                        }
                    }
                }
                else if ("datasource".equals(componentType))
                {
                    DatasourceCardConfig dsConfig = datasourceCardConfigService.selectByDashboardCardId(dashboardCard.getId());
                    if (dsConfig != null)
                    {
                        cardInfo.put("datasourceConfig", dsConfig);
                    }
                }

                cardList.add(cardInfo);
            }
            
            return success(cardList);
        }
        catch (Exception e)
        {
            log.error("获取看板卡片列表失败: dashboardId={}", id, e);
            return error("获取看板卡片列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存看板布局配置
     */
    @PreAuthorize("@ss.hasPermi('dashboard:edit')")
    @Log(title = "看板", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/layout")
    public AjaxResult saveLayout(@PathVariable("id") Long id, @RequestBody java.util.Map<String, Object> layoutData)
    {
        try
        {
            Dashboard dashboard = dashboardService.selectDashboardById(id);
            if (dashboard == null)
            {
                return error("看板不存在");
            }
            
            String layoutConfig = com.alibaba.fastjson2.JSON.toJSONString(layoutData);
            dashboard.setLayoutConfig(layoutConfig);
            
            @SuppressWarnings("unchecked")
            List<java.util.Map<String, Object>> cards = (List<java.util.Map<String, Object>>) layoutData.get("cards");
            List<java.util.Map<String, Object>> savedCards = new java.util.ArrayList<>();
            Set<Long> retainedDashboardCardIds = new HashSet<>();
            if (cards != null)
            {
                for (java.util.Map<String, Object> cardData : cards)
                {
                    Long dashboardCardId = toLong(cardData.get("dashboardCardId"));
                    Long cardId = toLong(cardData.get("cardId"));
                    String componentType = (String) cardData.getOrDefault("componentType", "chart");

                    if ("chart".equals(componentType) && cardId == null)
                    {
                        log.warn("跳过无效图表卡片（cardId为空）: {}", cardData);
                        continue;
                    }

                    DashboardCard dashboardCard = new DashboardCard();
                    dashboardCard.setId(dashboardCardId);
                    dashboardCard.setDashboardId(id);
                    dashboardCard.setCardId(cardId);
                    dashboardCard.setComponentType(componentType);
                    dashboardCard.setPositionX(toInt(cardData.get("positionX"), 0));
                    dashboardCard.setPositionY(toInt(cardData.get("positionY"), 0));
                    dashboardCard.setWidth(toInt(cardData.get("width"), 200));
                    dashboardCard.setHeight(toInt(cardData.get("height"), 150));
                    dashboardCard.setSortOrder(toInt(cardData.get("sortOrder"), 0));
                    dashboardCard.setStyleConfig((String) cardData.get("styleConfig"));
                    dashboardCard.setParentId(toLong(cardData.get("parentId")));
                    dashboardCard.setDecorationType((String) cardData.get("decorationType"));
                    dashboardCard.setCardName((String) cardData.get("cardName"));
                    
                    if (dashboardCardId != null && dashboardCardId > 0)
                    {
                        dashboardCardMapper.updateDashboardCard(dashboardCard);
                    }
                    else
                    {
                        dashboardCardMapper.insertDashboardCard(dashboardCard);
                    }
                    if (dashboardCard.getId() != null)
                    {
                        retainedDashboardCardIds.add(dashboardCard.getId());
                    }

                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> dsConfigData = (java.util.Map<String, Object>) cardData.get("datasourceConfig");
                    if ("datasource".equals(componentType) && dsConfigData != null)
                    {
                        Long dsConfigId = toLong(dsConfigData.get("id"));
                        DatasourceCardConfig dsConfig = new DatasourceCardConfig();
                        dsConfig.setId(dsConfigId);
                        dsConfig.setDashboardCardId(dashboardCard.getId());
                        dsConfig.setDatasourceId(toLong(dsConfigData.get("datasourceId")));
                        dsConfig.setQueryType((String) dsConfigData.get("queryType"));
                        dsConfig.setSqlTemplate((String) dsConfigData.get("sqlTemplate"));
                        dsConfig.setApiUrl((String) dsConfigData.get("apiUrl"));
                        dsConfig.setApiMethod((String) dsConfigData.get("apiMethod"));
                        dsConfig.setApiHeaders((String) dsConfigData.get("apiHeaders"));
                        dsConfig.setApiBody((String) dsConfigData.get("apiBody"));
                        dsConfig.setResponseDataPath((String) dsConfigData.get("responseDataPath"));
                        dsConfig.setChartType((String) dsConfigData.get("chartType"));
                        dsConfig.setColumnMapping(dsConfigData.get("columnMapping") instanceof String
                            ? (String) dsConfigData.get("columnMapping")
                            : com.alibaba.fastjson2.JSON.toJSONString(dsConfigData.get("columnMapping")));

                        if (dsConfigId != null && dsConfigId > 0)
                        {
                            datasourceCardConfigService.updateDatasourceCardConfig(dsConfig);
                        }
                        else
                        {
                            datasourceCardConfigService.insertDatasourceCardConfig(dsConfig);
                        }

                        java.util.Map<String, Object> savedCard = new java.util.HashMap<>(cardData);
                        savedCard.put("dashboardCardId", dashboardCard.getId());
                        java.util.Map<String, Object> savedDsConfig = new java.util.HashMap<>(dsConfigData);
                        savedDsConfig.put("id", dsConfig.getId());
                        savedDsConfig.put("dashboardCardId", dashboardCard.getId());
                        savedCard.put("datasourceConfig", savedDsConfig);
                        savedCards.add(savedCard);
                        continue;
                    }

                    java.util.Map<String, Object> savedCard = new java.util.HashMap<>(cardData);
                    savedCard.put("dashboardCardId", dashboardCard.getId());
                    savedCards.add(savedCard);
                }
            }

            // 删除本次布局中已移除的旧卡片，避免预览仍读取历史组件
            List<DashboardCard> existingCards = dashboardCardMapper.selectDashboardCardListByDashboardId(id);
            for (DashboardCard existingCard : existingCards)
            {
                Long existingCardId = existingCard.getId();
                if (existingCardId == null || retainedDashboardCardIds.contains(existingCardId))
                {
                    continue;
                }
                DatasourceCardConfig dsConfig = datasourceCardConfigService.selectByDashboardCardId(existingCardId);
                if (dsConfig != null && dsConfig.getId() != null)
                {
                    datasourceCardConfigService.deleteDatasourceCardConfigById(dsConfig.getId());
                }
                dashboardCardMapper.deleteDashboardCardById(existingCardId);
            }
            
            dashboardService.updateDashboard(dashboard);
            
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("msg", "布局保存成功");
            result.put("cards", savedCards);
            return success(result);
        }
        catch (Exception e)
        {
            log.error("保存看板布局失败: dashboardId={}", id, e);
            return error("保存看板布局失败: " + e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('dashboard:add')")
    @Log(title = "看板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Dashboard dashboard)
    {
        int rows = dashboardService.insertDashboard(dashboard);
        if (rows > 0) {
            return AjaxResult.success("操作成功", dashboard.getId());
        }
        return AjaxResult.error("新增看板失败");
    }

    @PreAuthorize("@ss.hasPermi('dashboard:edit')")
    @Log(title = "看板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Dashboard dashboard)
    {
        return toAjax(dashboardService.updateDashboard(dashboard));
    }

    @PreAuthorize("@ss.hasPermi('dashboard:remove')")
    @Log(title = "看板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(dashboardService.deleteDashboardByIds(ids));
    }

    @Autowired
    private dev.lhl.dashboard.mapper.DashboardCardMapper dashboardCardMapper;

    @Autowired
    private dev.lhl.dashboard.service.IDashboardRefreshService dashboardRefreshService;

    @Autowired
    private IDatasourceCardConfigService datasourceCardConfigService;

    @Autowired
    private IDashboardReportGenerateService dashboardReportGenerateService;

    @PreAuthorize("@ss.hasPermi('dashboard:refresh')")
    @Log(title = "看板", businessType = BusinessType.OTHER)
    @PostMapping("/refresh/{id}")
    public AjaxResult refresh(@PathVariable("id") Long id)
    {
        try
        {
            IDashboardRefreshService.RefreshResult result = dashboardRefreshService.refreshDashboard(id);
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("successCount", result.getSuccessCount());
            response.put("failCount", result.getFailCount());
            response.put("message", result.getMessage());
            response.put("refreshTime", new java.util.Date());
            
            return success(response);
        }
        catch (Exception e)
        {
            log.error("看板刷新失败: dashboardId={}", id, e);
            return error("看板刷新失败: " + e.getMessage());
        }
    }

    /**
     * 执行图表卡 SQL 并返回图表数据（用于展示与刷新，不落库）
     */
    @PreAuthorize("@ss.hasPermi('dashboard:query')")
    @GetMapping("/chart-card/{cardId}/data")
    public AjaxResult getChartCardData(@PathVariable("cardId") Long cardId)
    {
        Long userId = SecurityUtils.getUserId();
        java.util.Map<String, Object> data = dashboardRefreshService.getChartCardData(cardId, userId);
        if (data == null)
        {
            return error("图表卡不存在、无 SQL 或查询失败");
        }
        return success(data);
    }

    // ========== 图表卡片管理 ==========
    @PreAuthorize("@ss.hasPermi('dashboard:card:list')")
    @GetMapping("/card/list")
    public TableDataInfo listCard(ChartCard chartCard)
    {
        startPage();
        List<ChartCard> list = chartCardService.selectChartCardList(chartCard);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('dashboard:card:add')")
    @Log(title = "图表卡片", businessType = BusinessType.INSERT)
    @PostMapping("/card")
    public AjaxResult addCard(@RequestBody ChartCard chartCard)
    {
        return toAjax(chartCardService.insertChartCard(chartCard));
    }

    @PreAuthorize("@ss.hasPermi('dashboard:card:edit')")
    @Log(title = "图表卡片", businessType = BusinessType.UPDATE)
    @PutMapping("/card")
    public AjaxResult editCard(@RequestBody ChartCard chartCard)
    {
        return toAjax(chartCardService.updateChartCard(chartCard));
    }

    @PreAuthorize("@ss.hasPermi('dashboard:card:remove')")
    @Log(title = "图表卡片", businessType = BusinessType.DELETE)
    @DeleteMapping("/card/{ids}")
    public AjaxResult removeCard(@PathVariable Long[] ids)
    {
        return toAjax(chartCardService.deleteChartCardByIds(ids));
    }

    private static Long toLong(Object o)
    {
        return o == null ? null : ((Number) o).longValue();
    }

    private static Integer toInt(Object o, int defaultValue)
    {
        return o == null ? defaultValue : ((Number) o).intValue();
    }

    // ========== 背景配置 ==========

    @PreAuthorize("@ss.hasPermi('dashboard:query')")
    @GetMapping("/{id}/background")
    public AjaxResult getBackground(@PathVariable("id") Long id)
    {
        Dashboard dashboard = dashboardService.selectDashboardById(id);
        if (dashboard == null)
        {
            return error("看板不存在");
        }
        return success(dashboard.getBackgroundConfig());
    }

    @PreAuthorize("@ss.hasPermi('dashboard:edit')")
    @Log(title = "看板背景", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/background")
    public AjaxResult updateBackground(@PathVariable("id") Long id, @RequestBody java.util.Map<String, Object> backgroundConfig)
    {
        Dashboard dashboard = dashboardService.selectDashboardById(id);
        if (dashboard == null)
        {
            return error("看板不存在");
        }
        dashboard.setBackgroundConfig(com.alibaba.fastjson2.JSON.toJSONString(backgroundConfig));
        dashboardService.updateDashboard(dashboard);
        return success("背景配置保存成功");
    }

    // ========== 卡片组合 ==========

    @PreAuthorize("@ss.hasPermi('dashboard:edit')")
    @Log(title = "卡片组合", businessType = BusinessType.INSERT)
    @PostMapping("/{id}/group")
    public AjaxResult createGroup(@PathVariable("id") Long id, @RequestBody java.util.Map<String, Object> groupData)
    {
        try
        {
            @SuppressWarnings("unchecked")
            List<Number> cardIds = (List<Number>) groupData.get("cardIds");
            String groupName = (String) groupData.getOrDefault("groupName", "卡片组合");

            if (cardIds == null || cardIds.size() < 2)
            {
                return error("至少选择两个卡片进行组合");
            }

            DashboardCard groupCard = new DashboardCard();
            groupCard.setDashboardId(id);
            groupCard.setComponentType("group");
            groupCard.setCardName(groupName);
            groupCard.setPositionX(0);
            groupCard.setPositionY(0);
            groupCard.setWidth(800);
            groupCard.setHeight(600);
            dashboardCardMapper.insertDashboardCard(groupCard);

            for (Number childId : cardIds)
            {
                DashboardCard child = new DashboardCard();
                child.setId(childId.longValue());
                child.setParentId(groupCard.getId());
                dashboardCardMapper.updateDashboardCard(child);
            }

            return success(groupCard);
        }
        catch (Exception e)
        {
            log.error("创建卡片组合失败: dashboardId={}", id, e);
            return error("创建卡片组合失败: " + e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('dashboard:edit')")
    @Log(title = "解除组合", businessType = BusinessType.UPDATE)
    @PostMapping("/{dashboardId}/group/{groupId}/ungroup")
    public AjaxResult ungroupCards(@PathVariable("dashboardId") Long dashboardId, @PathVariable("groupId") Long groupId)
    {
        try
        {
            List<DashboardCard> children = dashboardCardMapper.selectChildCards(groupId);
            for (DashboardCard child : children)
            {
                child.setParentId(null);
                dashboardCardMapper.updateDashboardCard(child);
            }
            dashboardCardMapper.deleteDashboardCardById(groupId);
            return success("解除组合成功");
        }
        catch (Exception e)
        {
            log.error("解除组合失败: groupId={}", groupId, e);
            return error("解除组合失败: " + e.getMessage());
        }
    }

    // ========== 数据源卡片配置 ==========

    @PreAuthorize("@ss.hasPermi('dashboard:card:query')")
    @GetMapping("/datasource-card/{id}")
    public AjaxResult getDatasourceCardConfig(@PathVariable("id") Long id)
    {
        return success(datasourceCardConfigService.selectDatasourceCardConfigById(id));
    }

    @PreAuthorize("@ss.hasPermi('dashboard:card:add')")
    @Log(title = "数据源卡片", businessType = BusinessType.INSERT)
    @PostMapping("/datasource-card")
    public AjaxResult addDatasourceCardConfig(@RequestBody DatasourceCardConfig config)
    {
        return toAjax(datasourceCardConfigService.insertDatasourceCardConfig(config));
    }

    @PreAuthorize("@ss.hasPermi('dashboard:card:edit')")
    @Log(title = "数据源卡片", businessType = BusinessType.UPDATE)
    @PutMapping("/datasource-card")
    public AjaxResult editDatasourceCardConfig(@RequestBody DatasourceCardConfig config)
    {
        return toAjax(datasourceCardConfigService.updateDatasourceCardConfig(config));
    }

    @PreAuthorize("@ss.hasPermi('dashboard:card:remove')")
    @Log(title = "数据源卡片", businessType = BusinessType.DELETE)
    @DeleteMapping("/datasource-card/{id}")
    public AjaxResult removeDatasourceCardConfig(@PathVariable("id") Long id)
    {
        return toAjax(datasourceCardConfigService.deleteDatasourceCardConfigById(id));
    }

    @PreAuthorize("@ss.hasPermi('dashboard:card:query')")
    @PostMapping("/datasource-card/{id}/execute")
    public AjaxResult executeDatasourceCard(@PathVariable("id") Long id)
    {
        try
        {
            java.util.Map<String, Object> result = datasourceCardConfigService.executeQuery(id);
            return success(result);
        }
        catch (Exception e)
        {
            log.error("数据源卡片查询执行失败: configId={}", id, e);
            return error("查询执行失败: " + e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('dashboard:card:query')")
    @PostMapping("/datasource-card/preview")
    public AjaxResult previewDatasourceQuery(@RequestBody DatasourceCardConfig config)
    {
        try
        {
            java.util.Map<String, Object> result = datasourceCardConfigService.previewQuery(config);
            return success(result);
        }
        catch (Exception e)
        {
            log.error("数据源查询预览失败", e);
            return error("查询预览失败: " + e.getMessage());
        }
    }

    // ========== 报表生成（LLM 驱动大屏一键生成） ==========

    @PreAuthorize("@ss.hasPermi('dashboard:edit')")
    @Log(title = "看板报表生成", businessType = BusinessType.OTHER)
    @PostMapping("/report/generate")
    public AjaxResult generateReport(@RequestBody ReportGenerateRequest request)
    {
        try
        {
            if (request == null || request.getPrompt() == null || request.getPrompt().isBlank())
            {
                return error("请填写大屏描述（prompt）");
            }
            Long userId = SecurityUtils.getUserId();
            ReportGenerateResult result = dashboardReportGenerateService.generateReport(request, userId);
            if (result.getErrorCode() != null)
            {
                return error(result.getErrorMessage() != null ? result.getErrorMessage() : "报表生成失败");
            }
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("cards", result.getCards());
            data.put("layout", result.getLayout());
            return success(data);
        }
        catch (Exception e)
        {
            log.error("报表生成失败", e);
            return error("报表生成失败，请重试或简化描述: " + e.getMessage());
        }
    }

    /**
     * 报表生成 SSE 流式接口：实时推送进度与每张卡片，避免长耗时超时
     */
    @PreAuthorize("@ss.hasPermi('dashboard:edit')")
    @Log(title = "看板报表生成(SSE)", businessType = BusinessType.OTHER)
    @PostMapping(value = "/report/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateReportStream(@RequestBody ReportGenerateRequest request)
    {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 分钟
        Long userId = SecurityUtils.getUserId();
        // 异步线程中无法从 ThreadLocal 获取登录用户，需把当前请求的认证上下文传入工作线程
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ReportGenerateProgressCallback callback = new ReportGenerateProgressCallback()
        {
            @Override
            public void onMessage(String message)
            {
                try
                {
                    emitter.send(SseEmitter.event().name("message").data(message));
                }
                catch (Exception e)
                {
                    log.warn("SSE send message failed", e);
                }
            }

            @Override
            public void onCardReady(int index, Map<String, Object> card, Map<String, Object> layoutItem)
            {
                try
                {
                    java.util.Map<String, Object> payload = new java.util.HashMap<>();
                    payload.put("index", index);
                    payload.put("card", card);
                    payload.put("layoutItem", layoutItem);
                    emitter.send(SseEmitter.event().name("card").data(com.alibaba.fastjson2.JSON.toJSONString(payload)));
                }
                catch (Exception e)
                {
                    log.warn("SSE send card failed", e);
                }
            }

            @Override
            public void onComplete()
            {
                try
                {
                    emitter.send(SseEmitter.event().name("done").data("ok"));
                    emitter.complete();
                }
                catch (Exception e)
                {
                    log.warn("SSE complete failed", e);
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onError(String code, String message)
            {
                try
                {
                    java.util.Map<String, Object> err = new java.util.HashMap<>();
                    err.put("code", code);
                    err.put("message", message);
                    emitter.send(SseEmitter.event().name("error").data(com.alibaba.fastjson2.JSON.toJSONString(err)));
                    emitter.complete();
                }
                catch (Exception e)
                {
                    log.warn("SSE error send failed", e);
                    emitter.completeWithError(e);
                }
            }
        };

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor(r ->
        {
            Thread t = new Thread(r, "report-generate-sse");
            t.setDaemon(false);
            return t;
        });
        executor.execute(() ->
        {
            try
            {
                if (auth != null) {
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
                ReportGenerateResult result = dashboardReportGenerateService.generateReport(request, userId, callback);
                if (result.getErrorCode() != null && !"ok".equals(result.getErrorCode()))
                {
                    callback.onError(result.getErrorCode(), result.getErrorMessage() != null ? result.getErrorMessage() : "生成失败");
                }
            }
            catch (Exception e)
            {
                log.error("报表生成 SSE 执行异常", e);
                callback.onError("INTERNAL_ERROR", e.getMessage() != null ? e.getMessage() : "服务异常");
            }
            finally
            {
                SecurityContextHolder.clearContext();
                executor.shutdown();
            }
        });

        emitter.onTimeout(() ->
        {
            executor.shutdownNow();
            emitter.complete();
        });
        emitter.onCompletion(executor::shutdown);
        return emitter;
    }
}
