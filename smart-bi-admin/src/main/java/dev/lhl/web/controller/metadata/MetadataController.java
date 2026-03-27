package dev.lhl.web.controller.metadata;

import java.util.List;

import dev.lhl.common.utils.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import dev.lhl.common.annotation.Log;
import dev.lhl.common.core.controller.BaseController;
import dev.lhl.common.core.domain.AjaxResult;
import dev.lhl.common.core.page.TableDataInfo;
import dev.lhl.common.enums.BusinessType;
import dev.lhl.metadata.domain.*;
import dev.lhl.metadata.domain.dto.AutocompleteItem;
import dev.lhl.metadata.service.IMetadataAutocompleteService;
import dev.lhl.metadata.service.IMetadataService;
import dev.lhl.web.domain.vo.TableMetadataVO;
import dev.lhl.web.domain.vo.FieldMetadataVO;
import dev.lhl.web.domain.vo.AmbiguityRecordVO;
import dev.lhl.web.service.MetadataTableListService;
import dev.lhl.query.domain.LlmAudit;
import dev.lhl.query.service.ILlmAuditService;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 元数据Controller
 *
 * @author smart-bi
 */
@RestController
@RequestMapping("/api/metadata")
public class MetadataController extends BaseController
{
    @Autowired
    private IMetadataService metadataService;

    @Autowired(required = false)
    private MetadataTableListService metadataTableListService;

    @Autowired(required = false)
    private ILlmAuditService llmAuditService;

    @Autowired(required = false)
    private IMetadataAutocompleteService metadataAutocompleteService;

    // ========== 元数据自动补全（问数输入联想） ==========
    @PreAuthorize("@ss.hasPermi('query:execute') or @ss.hasPermi('metadata:table:list')")
    @GetMapping("/autocomplete")
    public AjaxResult autocomplete(@RequestParam(value = "keyword", required = false) String keyword,
                                   @RequestParam(value = "limit", defaultValue = "10") Integer limit)
    {
        if (metadataAutocompleteService == null)
        {
            return success(new ArrayList<>());
        }
        Long userId = SecurityUtils.getUserId();
        int max = (limit != null && limit > 0 && limit <= 20) ? limit : 10;
        List<AutocompleteItem> list = metadataAutocompleteService.search(keyword, userId, max);
        return success(list);
    }

    // ========== 业务域管理 ==========
    @PreAuthorize("@ss.hasPermi('metadata:domain:list')")
    @GetMapping("/domain/list")
    public TableDataInfo listDomain(BusinessDomain businessDomain)
    {
        startPage();
        List<BusinessDomain> list = metadataService.selectBusinessDomainList(businessDomain);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('metadata:domain:query')")
    @GetMapping("/domain/{id}")
    public AjaxResult getDomain(@PathVariable("id") Long id)
    {
        return success(metadataService.selectBusinessDomainById(id));
    }

    @PreAuthorize("@ss.hasPermi('metadata:domain:add')")
    @Log(title = "业务域", businessType = BusinessType.INSERT)
    @PostMapping("/domain")
    public AjaxResult addDomain(@RequestBody BusinessDomain businessDomain)
    {
        return toAjax(metadataService.insertBusinessDomain(businessDomain));
    }

    @PreAuthorize("@ss.hasPermi('metadata:domain:edit')")
    @Log(title = "业务域", businessType = BusinessType.UPDATE)
    @PutMapping("/domain")
    public AjaxResult editDomain(@RequestBody BusinessDomain businessDomain)
    {
        return toAjax(metadataService.updateBusinessDomain(businessDomain));
    }

    @PreAuthorize("@ss.hasPermi('metadata:domain:remove')")
    @Log(title = "业务域", businessType = BusinessType.DELETE)
    @DeleteMapping("/domain/{ids}")
    public AjaxResult removeDomain(@PathVariable Long[] ids)
    {
        return toAjax(metadataService.deleteBusinessDomainByIds(ids));
    }

    // ========== 表元数据管理 ==========
    @PreAuthorize("@ss.hasPermi('metadata:table:list')")
    @GetMapping("/table/list")
    public TableDataInfo listTable(TableMetadata tableMetadata)
    {
        startPage();
        List<TableMetadata> list = metadataService.selectTableMetadataList(tableMetadata);
        List<TableMetadataVO> voList = (metadataTableListService != null)
            ? metadataTableListService.enrichWithAudit(list)
            : buildSimpleTableVoList(list);
        TableDataInfo info = getDataTable(list);
        info.setRows(voList);
        return info;
    }

    private static List<TableMetadataVO> buildSimpleTableVoList(List<TableMetadata> list)
    {
        List<TableMetadataVO> voList = new java.util.ArrayList<>(list.size());
        for (TableMetadata t : list)
        {
            TableMetadataVO vo = new TableMetadataVO();
            vo.setId(t.getId());
            vo.setTableName(t.getTableName());
            vo.setTableComment(t.getTableComment());
            vo.setBusinessDescription(t.getBusinessDescription());
            vo.setDomainId(t.getDomainId());
            vo.setTableUsage(t.getTableUsage());
            vo.setNl2sqlVisibilityLevel(t.getNl2sqlVisibilityLevel());
            vo.setGrainDesc(t.getGrainDesc());
            vo.setUpdateTime(t.getUpdateTime());
            vo.setErrorCount(0);
            voList.add(vo);
        }
        return voList;
    }

    @PreAuthorize("@ss.hasPermi('metadata:table:query')")
    @GetMapping("/table/{id}")
    public AjaxResult getTable(@PathVariable("id") Long id)
    {
        return success(metadataService.selectTableMetadataById(id));
    }

    @PreAuthorize("@ss.hasPermi('metadata:table:add')")
    @Log(title = "表元数据", businessType = BusinessType.INSERT)
    @PostMapping("/table")
    public AjaxResult addTable(@RequestBody TableMetadata tableMetadata)
    {
        return toAjax(metadataService.insertTableMetadata(tableMetadata));
    }

    @PreAuthorize("@ss.hasPermi('metadata:table:edit')")
    @Log(title = "表元数据", businessType = BusinessType.UPDATE)
    @PutMapping("/table")
    public AjaxResult editTable(@RequestBody TableMetadata tableMetadata)
    {
        return toAjax(metadataService.updateTableMetadata(tableMetadata));
    }

    /** 行内编辑单条表（用途/可见性/粒度），带乐观锁，冲突返回 409 */
    @PreAuthorize("@ss.hasPermi('metadata:table:edit')")
    @Log(title = "表元数据", businessType = BusinessType.UPDATE)
    @PutMapping("/table/{id}")
    public AjaxResult updateTableById(@PathVariable("id") Long id, @RequestBody TableMetadata tableMetadata)
    {
        tableMetadata.setId(id);
        int n = metadataService.updateTableMetadataWithOptimisticLock(tableMetadata);
        if (n == 0)
            return AjaxResult.error(HttpStatus.CONFLICT.value(), "数据已被他人修改，请刷新后重试");
        return success();
    }

    /** 批量更新表的用途与可见性 */
    @PreAuthorize("@ss.hasPermi('metadata:table:edit')")
    @Log(title = "表元数据", businessType = BusinessType.UPDATE)
    @PutMapping("/table/batch")
    public AjaxResult batchUpdateTable(@RequestBody java.util.Map<String, Object> body)
    {
        @SuppressWarnings("unchecked")
        List<Number> idList = (List<Number>) body.get("ids");
        if (idList == null || idList.isEmpty())
            return error("ids 不能为空");
        Long[] ids = idList.stream().map(Number::longValue).toArray(Long[]::new);
        String tableUsage = (String) body.get("tableUsage");
        String nl2sqlVisibilityLevel = (String) body.get("nl2sqlVisibilityLevel");
        int n = metadataService.batchUpdateTableMetadata(ids, tableUsage, nl2sqlVisibilityLevel);
        return success(n);
    }

    /** 问题表高亮配置（时间窗口天数、错误次数阈值），供前端展示与判断 */
    @PreAuthorize("@ss.hasPermi('metadata:table:list')")
    @GetMapping("/table/problem-config")
    public AjaxResult getTableProblemConfig()
    {
        if (metadataTableListService == null)
            return success(new java.util.HashMap<String, Object>() {{ put("windowDays", 30); put("errorCountThreshold", 3); }});
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("windowDays", metadataTableListService.getProblemTableWindowDays());
        m.put("errorCountThreshold", metadataTableListService.getProblemTableErrorThreshold());
        return success(m);
    }

    /** 更新问题表高亮配置（时间窗口天数、错误次数阈值） */
    @PreAuthorize("@ss.hasPermi('metadata:table:edit')")
    @Log(title = "问题表高亮配置", businessType = BusinessType.UPDATE)
    @PutMapping("/table/problem-config")
    public AjaxResult updateTableProblemConfig(@RequestBody java.util.Map<String, Object> body)
    {
        if (metadataTableListService == null)
            return error("服务不可用");
        Integer windowDays = body.get("windowDays") != null ? ((Number) body.get("windowDays")).intValue() : null;
        Integer errorCountThreshold = body.get("errorCountThreshold") != null ? ((Number) body.get("errorCountThreshold")).intValue() : null;
        if (windowDays == null && errorCountThreshold == null)
            return error("请至少传入 windowDays 或 errorCountThreshold");
        metadataTableListService.updateProblemTableConfig(windowDays, errorCountThreshold);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('metadata:table:remove')")
    @Log(title = "表元数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/table/{ids}")
    public AjaxResult removeTable(@PathVariable Long[] ids)
    {
        return toAjax(metadataService.deleteTableMetadataByIds(ids));
    }

    // ========== 字段元数据管理 ==========
    @PreAuthorize("@ss.hasPermi('metadata:field:list')")
    @GetMapping("/field/list")
    public TableDataInfo listField(FieldMetadata fieldMetadata)
    {
        startPage();
        List<FieldMetadata> list = metadataService.selectFieldMetadataList(fieldMetadata);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('metadata:field:list')")
    @GetMapping("/field/table/{tableId}")
    public AjaxResult listFieldByTable(@PathVariable("tableId") Long tableId)
    {
        List<FieldMetadata> list = metadataService.selectFieldMetadataListByTableId(tableId);
        List<Long> fieldIds = list.stream().map(FieldMetadata::getId).filter(id -> id != null).collect(Collectors.toList());
        List<FieldAlias> allAliases = metadataService.selectFieldAliasListByFieldIds(fieldIds);
        java.util.Map<Long, List<FieldAlias>> aliasesByFieldId = allAliases.stream().collect(Collectors.groupingBy(FieldAlias::getFieldId));
        List<FieldMetadataVO> voList = new ArrayList<>(list.size());
        for (FieldMetadata f : list)
        {
            FieldMetadataVO vo = toFieldMetadataVO(f);
            List<FieldAlias> aliases = aliasesByFieldId.getOrDefault(f.getId(), java.util.Collections.emptyList());
            vo.setAliases(aliases.stream().map(a -> {
                FieldMetadataVO.FieldAliasItem item = new FieldMetadataVO.FieldAliasItem();
                item.setId(a.getId());
                item.setAlias(a.getAlias());
                item.setSource(a.getSource());
                return item;
            }).collect(Collectors.toList()));
            voList.add(vo);
        }
        return success(voList);
    }

    private static FieldMetadataVO toFieldMetadataVO(FieldMetadata f)
    {
        FieldMetadataVO vo = new FieldMetadataVO();
        vo.setId(f.getId());
        vo.setTableId(f.getTableId());
        vo.setFieldName(f.getFieldName());
        vo.setFieldType(f.getFieldType());
        vo.setFieldComment(f.getFieldComment());
        vo.setBusinessAlias(f.getBusinessAlias());
        vo.setBusinessDescription(f.getBusinessDescription());
        vo.setUsageType(f.getUsageType());
        vo.setSemanticType(f.getSemanticType());
        vo.setUnit(f.getUnit());
        vo.setDefaultAggFunc(f.getDefaultAggFunc());
        vo.setAllowedAggFuncs(f.getAllowedAggFuncs());
        vo.setNl2sqlPriority(f.getNl2sqlPriority());
        vo.setSensitiveLevel(f.getSensitiveLevel());
        vo.setExposurePolicy(f.getExposurePolicy());
        vo.setUpdateTime(f.getUpdateTime());
        return vo;
    }

    /** 行内编辑单条字段（用途/语义类型/单位/聚合/优先级/敏感/曝光），带乐观锁 */
    @PreAuthorize("@ss.hasPermi('metadata:field:edit')")
    @Log(title = "字段元数据", businessType = BusinessType.UPDATE)
    @PutMapping("/field/{id}")
    public AjaxResult updateFieldById(@PathVariable("id") Long id, @RequestBody FieldMetadata fieldMetadata)
    {
        fieldMetadata.setId(id);
        int n = metadataService.updateFieldMetadataWithOptimisticLock(fieldMetadata);
        if (n == 0)
            return AjaxResult.error(HttpStatus.CONFLICT.value(), "数据已被他人修改，请刷新后重试");
        return success();
    }

    /** 新增字段别名，(field_id, alias) 重复返回 4xx */
    @PreAuthorize("@ss.hasPermi('metadata:field:edit')")
    @Log(title = "字段别名", businessType = BusinessType.INSERT)
    @PostMapping("/field/{fieldId}/alias")
    public AjaxResult addFieldAlias(@PathVariable("fieldId") Long fieldId, @RequestBody java.util.Map<String, Object> body)
    {
        String alias = (String) body.get("alias");
        String source = (String) body.get("source");
        if (alias == null || alias.trim().isEmpty())
            return error("别名不能为空");
        FieldAlias fa = new FieldAlias();
        fa.setFieldId(fieldId);
        fa.setAlias(alias.trim());
        fa.setSource(source != null ? source : "HUMAN");
        try
        {
            FieldAlias inserted = metadataService.insertFieldAlias(fa);
            return success(inserted);
        }
        catch (dev.lhl.common.exception.ServiceException e)
        {
            return error(e.getMessage());
        }
    }

    /** 删除字段别名 */
    @PreAuthorize("@ss.hasPermi('metadata:field:edit')")
    @Log(title = "字段别名", businessType = BusinessType.DELETE)
    @DeleteMapping("/field/{fieldId}/alias/{aliasId}")
    public AjaxResult removeFieldAlias(@PathVariable("fieldId") Long fieldId, @PathVariable("aliasId") Long aliasId)
    {
        return toAjax(metadataService.deleteFieldAliasById(aliasId));
    }

    /** 推荐别名列表（供前端采纳） */
    @PreAuthorize("@ss.hasPermi('metadata:field:list')")
    @GetMapping("/field/{fieldId}/alias/suggestions")
    public AjaxResult getFieldAliasSuggestions(@PathVariable("fieldId") Long fieldId)
    {
        return success(metadataService.getFieldAliasSuggestions(fieldId));
    }

    /** 别名冲突：同一 alias 出现在其他表/字段 */
    @PreAuthorize("@ss.hasPermi('metadata:field:list')")
    @GetMapping("/alias/conflicts")
    public AjaxResult getAliasConflicts(@RequestParam("alias") String alias, @RequestParam(value = "excludeFieldId", required = false) Long excludeFieldId)
    {
        List<AliasConflictItem> list = metadataService.findAliasConflicts(alias, excludeFieldId);
        return success(list);
    }

    @PreAuthorize("@ss.hasPermi('metadata:field:query')")
    @GetMapping("/field/{id}")
    public AjaxResult getField(@PathVariable("id") Long id)
    {
        return success(metadataService.selectFieldMetadataById(id));
    }

    @PreAuthorize("@ss.hasPermi('metadata:field:add')")
    @Log(title = "字段元数据", businessType = BusinessType.INSERT)
    @PostMapping("/field")
    public AjaxResult addField(@RequestBody FieldMetadata fieldMetadata)
    {
        return toAjax(metadataService.insertFieldMetadata(fieldMetadata));
    }

    @PreAuthorize("@ss.hasPermi('metadata:field:edit')")
    @Log(title = "字段元数据", businessType = BusinessType.UPDATE)
    @PutMapping("/field")
    public AjaxResult editField(@RequestBody FieldMetadata fieldMetadata)
    {
        return toAjax(metadataService.updateFieldMetadata(fieldMetadata));
    }

    @PreAuthorize("@ss.hasPermi('metadata:field:remove')")
    @Log(title = "字段元数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/field/{ids}")
    public AjaxResult removeField(@PathVariable Long[] ids)
    {
        return toAjax(metadataService.deleteFieldMetadataByIds(ids));
    }

    // ========== 原子指标管理 ==========
    @PreAuthorize("@ss.hasPermi('metadata:metric:list')")
    @GetMapping("/metric/list")
    public TableDataInfo listMetric(AtomicMetric atomicMetric)
    {
        startPage();
        List<AtomicMetric> list = metadataService.selectAtomicMetricList(atomicMetric);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('metadata:metric:query')")
    @GetMapping("/metric/{id}")
    public AjaxResult getMetric(@PathVariable("id") Long id)
    {
        return success(metadataService.selectAtomicMetricById(id));
    }

    @PreAuthorize("@ss.hasPermi('metadata:metric:add')")
    @Log(title = "原子指标", businessType = BusinessType.INSERT)
    @PostMapping("/metric")
    public AjaxResult addMetric(@RequestBody AtomicMetric atomicMetric)
    {
        return toAjax(metadataService.insertAtomicMetric(atomicMetric));
    }

    @PreAuthorize("@ss.hasPermi('metadata:metric:edit')")
    @Log(title = "原子指标", businessType = BusinessType.UPDATE)
    @PutMapping("/metric")
    public AjaxResult editMetric(@RequestBody AtomicMetric atomicMetric)
    {
        return toAjax(metadataService.updateAtomicMetric(atomicMetric));
    }

    @PreAuthorize("@ss.hasPermi('metadata:metric:remove')")
    @Log(title = "原子指标", businessType = BusinessType.DELETE)
    @DeleteMapping("/metric/{ids}")
    public AjaxResult removeMetric(@PathVariable Long[] ids)
    {
        return toAjax(metadataService.deleteAtomicMetricByIds(ids));
    }

    // ========== 表关系（推荐 join）管理 ==========
    @PreAuthorize("@ss.hasPermi('metadata:relation:list')")
    @GetMapping("/relation/list")
    public TableDataInfo listRelation(TableRelation tableRelation)
    {
        startPage();
        List<TableRelation> list = metadataService.selectTableRelationList(tableRelation);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('metadata:relation:query')")
    @GetMapping("/relation/{id}")
    public AjaxResult getRelation(@PathVariable("id") Long id)
    {
        return success(metadataService.selectTableRelationById(id));
    }

    @PreAuthorize("@ss.hasPermi('metadata:relation:add')")
    @Log(title = "表关系", businessType = BusinessType.INSERT)
    @PostMapping("/relation")
    public AjaxResult addRelation(@RequestBody TableRelation tableRelation)
    {
        return toAjax(metadataService.insertTableRelation(tableRelation));
    }

    @PreAuthorize("@ss.hasPermi('metadata:relation:edit')")
    @Log(title = "表关系", businessType = BusinessType.UPDATE)
    @PutMapping("/relation/{id}")
    public AjaxResult editRelation(@PathVariable("id") Long id, @RequestBody TableRelation tableRelation)
    {
        tableRelation.setId(id);
        return toAjax(metadataService.updateTableRelation(tableRelation));
    }

    @PreAuthorize("@ss.hasPermi('metadata:relation:remove')")
    @Log(title = "表关系", businessType = BusinessType.DELETE)
    @DeleteMapping("/relation/{ids}")
    public AjaxResult removeRelation(@PathVariable Long[] ids)
    {
        return toAjax(metadataService.deleteTableRelationByIds(ids));
    }

    /** 血缘图数据：白名单表（NORMAL/PREFERRED）+ 表关系，仅返回左右表均在白名单内的关系 */
    @PreAuthorize("@ss.hasPermi('metadata:relation:list')")
    @GetMapping("/relation/graph-data")
    public AjaxResult getRelationGraphData()
    {
        List<TableMetadata> tables = metadataService.selectTableMetadataListForNl2Sql(null);
        List<TableRelation> relations = metadataService.selectTableRelationList(new TableRelation());
        java.util.Set<String> tableNames = tables.stream()
            .map(t -> t.getTableName() != null ? t.getTableName().trim() : null)
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());
        List<TableRelation> filtered = relations.stream()
            .filter(r -> tableNames.contains(r.getLeftTable()) && tableNames.contains(r.getRightTable()))
            .collect(Collectors.toList());
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("tables", tables);
        data.put("relations", filtered);
        return success(data);
    }

    // ========== 维度管理 ==========
    @PreAuthorize("@ss.hasPermi('metadata:dimension:list')")
    @GetMapping("/dimension/list")
    public TableDataInfo listDimension(Dimension dimension)
    {
        startPage();
        List<Dimension> list = metadataService.selectDimensionList(dimension);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('metadata:dimension:query')")
    @GetMapping("/dimension/{id}")
    public AjaxResult getDimension(@PathVariable("id") Long id)
    {
        return success(metadataService.selectDimensionById(id));
    }

    @PreAuthorize("@ss.hasPermi('metadata:dimension:add')")
    @Log(title = "维度", businessType = BusinessType.INSERT)
    @PostMapping("/dimension")
    public AjaxResult addDimension(@RequestBody Dimension dimension)
    {
        return toAjax(metadataService.insertDimension(dimension));
    }

    @PreAuthorize("@ss.hasPermi('metadata:dimension:edit')")
    @Log(title = "维度", businessType = BusinessType.UPDATE)
    @PutMapping("/dimension")
    public AjaxResult editDimension(@RequestBody Dimension dimension)
    {
        return toAjax(metadataService.updateDimension(dimension));
    }

    @PreAuthorize("@ss.hasPermi('metadata:dimension:remove')")
    @Log(title = "维度", businessType = BusinessType.DELETE)
    @DeleteMapping("/dimension/{ids}")
    public AjaxResult removeDimension(@PathVariable Long[] ids)
    {
        return toAjax(metadataService.deleteDimensionByIds(ids));
    }

    // ========== 歧义优化 / 智能标注 ==========
    /** 歧义列表：分页筛选 errorCategory、tableName、startTime、endTime、processStatus */
    @PreAuthorize("@ss.hasPermi('metadata:ambiguity:list')")
    @GetMapping("/ambiguity/list")
    public TableDataInfo listAmbiguity(
        @RequestParam(value = "errorCategory", required = false) String errorCategory,
        @RequestParam(value = "tableName", required = false) String tableName,
        @RequestParam(value = "startTime", required = false) java.util.Date startTime,
        @RequestParam(value = "endTime", required = false) java.util.Date endTime,
        @RequestParam(value = "processStatus", required = false) String processStatus)
    {
        if (llmAuditService == null)
            return getDataTable(new ArrayList<>());
        startPage();
        List<LlmAudit> list = llmAuditService.listForAmbiguity(errorCategory, tableName, startTime, endTime, processStatus);
        List<AmbiguityRecordVO> voList = new ArrayList<>(list.size());
        for (LlmAudit a : list)
        {
            AmbiguityRecordVO vo = new AmbiguityRecordVO();
            vo.setId(a.getId());
            vo.setQueryId(a.getQueryRecordId());
            vo.setOriginalQuestion(a.getOriginalQuestion());
            vo.setGeneratedSql(a.getGeneratedSql());
            vo.setErrorCategory(a.getErrorCategory());
            vo.setProcessStatus(a.getProcessStatus() != null ? a.getProcessStatus() : "PENDING");
            vo.setCreateTime(a.getCreateTime());
            vo.setUserId(a.getUserId());
            vo.setInvolvedTables(AmbiguityRecordVO.parseInvolvedTables(a.getGeneratedSql()));
            voList.add(vo);
        }
        TableDataInfo info = getDataTable(list);
        info.setRows(voList);
        return info;
    }

    /** 标记歧义记录为已处理 */
    @PreAuthorize("@ss.hasPermi('metadata:ambiguity:edit')")
    @Log(title = "歧义优化", businessType = BusinessType.UPDATE)
    @PutMapping("/ambiguity/{id}/resolve")
    public AjaxResult resolveAmbiguity(@PathVariable("id") Long id)
    {
        if (llmAuditService == null)
            return error("审计服务不可用");
        int n = llmAuditService.resolveAmbiguity(id);
        return n > 0 ? success() : error("更新失败");
    }

    /** 歧义按表汇总（用于歧义优化页汇总视图，默认最近 90 天） */
    @PreAuthorize("@ss.hasPermi('metadata:ambiguity:list')")
    @GetMapping("/ambiguity/summary")
    public AjaxResult getAmbiguitySummary(
        @RequestParam(value = "startTime", required = false) java.util.Date startTime,
        @RequestParam(value = "endTime", required = false) java.util.Date endTime)
    {
        if (llmAuditService == null)
            return success(new ArrayList<>());
        return success(llmAuditService.getAmbiguitySummary(startTime, endTime));
    }
}
