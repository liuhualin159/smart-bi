package dev.lhl.web.controller.quality;

import dev.lhl.common.annotation.Log;
import dev.lhl.common.core.controller.BaseController;
import dev.lhl.common.core.domain.AjaxResult;
import dev.lhl.common.core.page.TableDataInfo;
import dev.lhl.common.enums.BusinessType;
import dev.lhl.metadata.service.IMetadataService;
import dev.lhl.metadata.domain.TableMetadata;
import dev.lhl.quality.domain.BiQualityRule;
import dev.lhl.quality.domain.RuleExecutionResult;
import dev.lhl.quality.service.IBiQualityRuleService;
import dev.lhl.quality.service.IQualityRuleEngine;
import dev.lhl.quality.service.IQualityRuleTestService;
import dev.lhl.quality.service.IQualityScoreService;
import dev.lhl.quality.service.IQualityAlertService;
import dev.lhl.quality.domain.BiQualityScore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据质量规则 Controller（CRUD）
 *
 * @author smart-bi
 */
@RestController
@RequestMapping("/api/quality/rule")
public class QualityRuleController extends BaseController {

    @Autowired
    private IBiQualityRuleService biQualityRuleService;

    @Autowired
    private IQualityRuleEngine qualityRuleEngine;

    @Autowired(required = false)
    private IQualityRuleTestService qualityRuleTestService;

    @Autowired(required = false)
    private IMetadataService metadataService;

    @Autowired(required = false)
    private IQualityScoreService qualityScoreService;

    @Autowired(required = false)
    private IQualityAlertService qualityAlertService;

    /**
     * 查询规则列表
     */
    @PreAuthorize("@ss.hasPermi('bi:quality:list')")
    @GetMapping("/list")
    public TableDataInfo list(BiQualityRule query) {
        startPage();
        List<BiQualityRule> list = biQualityRuleService.selectList(query);
        return getDataTable(list);
    }

    /**
     * 获取规则详情
     */
    @PreAuthorize("@ss.hasPermi('bi:quality:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(biQualityRuleService.selectById(id));
    }

    /**
     * 新增规则
     */
    @PreAuthorize("@ss.hasPermi('bi:quality:add')")
    @Log(title = "数据质量规则", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BiQualityRule rule) {
        return toAjax(biQualityRuleService.insert(rule));
    }

    /**
     * 修改规则
     */
    @PreAuthorize("@ss.hasPermi('bi:quality:edit')")
    @Log(title = "数据质量规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BiQualityRule rule) {
        return toAjax(biQualityRuleService.updateById(rule));
    }

    /**
     * 删除规则
     */
    @PreAuthorize("@ss.hasPermi('bi:quality:remove')")
    @Log(title = "数据质量规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(biQualityRuleService.deleteByIds(ids));
    }

    /**
     * 执行单条规则
     */
    @PreAuthorize("@ss.hasPermi('bi:quality:list')")
    @PostMapping("/execute/{id}")
    public AjaxResult executeRule(@PathVariable("id") Long id) {
        BiQualityRule rule = biQualityRuleService.selectById(id);
        if (rule == null) return error("规则不存在");
        TableMetadata table = metadataService != null ? metadataService.selectTableMetadataById(rule.getTableId()) : null;
        if (table == null) return error("表不存在");
        RuleExecutionResult result = qualityRuleEngine.executeRule(rule, table.getTableName());
        return success(result);
    }

    /**
     * 规则测试（抽样执行，返回 passed/failed 统计）
     */
    @PreAuthorize("@ss.hasPermi('bi:quality:list')")
    @Log(title = "规则测试", businessType = BusinessType.OTHER)
    @PostMapping("/test")
    public AjaxResult testRules(@RequestBody Map<String, Object> params) {
        if (qualityRuleTestService == null) return error("规则测试服务未配置");
        Object tableIdObj = params.get("tableId");
        Object sampleSizeObj = params.get("sampleSize");
        Long tableId = tableIdObj != null ? Long.valueOf(String.valueOf(tableIdObj)) : null;
        Integer sampleSize = sampleSizeObj != null ? Integer.valueOf(String.valueOf(sampleSizeObj)) : 1000;
        if (tableId == null) return error("tableId 必填");
        Map<String, Object> result = qualityRuleTestService.runRuleTest(tableId, sampleSize);
        return success(result);
    }

    /**
     * 计算表级质量评分
     */
    @PreAuthorize("@ss.hasPermi('bi:quality:list')")
    @Log(title = "计算质量评分", businessType = BusinessType.OTHER)
    @PostMapping("/score/calculate")
    public AjaxResult calculateScore(@RequestBody Map<String, Object> params) {
        if (qualityScoreService == null) return error("评分服务未配置");
        Object tableIdObj = params.get("tableId");
        Long tableId = tableIdObj != null ? Long.valueOf(String.valueOf(tableIdObj)) : null;
        if (tableId == null) return error("tableId 必填");
        BiQualityScore score = qualityScoreService.calculateAndSaveTableScore(tableId);
        return score != null ? success(score) : error("计算失败");
    }

    /**
     * 查询表评分历史
     */
    @PreAuthorize("@ss.hasPermi('bi:quality:query')")
    @GetMapping("/score/history")
    public AjaxResult scoreHistory(Long tableId) {
        if (qualityScoreService == null || tableId == null) return error("参数无效");
        return success(qualityScoreService.getScoreHistory(tableId));
    }

    /**
     * 触发告警检查
     */
    @PreAuthorize("@ss.hasPermi('bi:quality:list')")
    @Log(title = "质量告警检查", businessType = BusinessType.OTHER)
    @PostMapping("/alert/check")
    public AjaxResult checkAlert(@RequestBody Map<String, Object> params) {
        if (qualityAlertService == null) return error("告警服务未配置");
        Object tableIdObj = params.get("tableId");
        Object thresholdObj = params.get("scoreThreshold");
        Long tableId = tableIdObj != null ? Long.valueOf(String.valueOf(tableIdObj)) : null;
        Integer threshold = thresholdObj != null ? Integer.valueOf(String.valueOf(thresholdObj)) : 60;
        int sent = qualityAlertService.checkAndAlert(tableId, threshold);
        return success(Map.of("alertsSent", sent));
    }
}
