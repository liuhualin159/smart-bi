package dev.lhl.web.controller.metadata;

import dev.lhl.common.core.controller.BaseController;
import dev.lhl.common.core.domain.AjaxResult;
import dev.lhl.common.core.page.TableDataInfo;
import dev.lhl.common.utils.SecurityUtils;
import dev.lhl.query.domain.FewshotExample;
import dev.lhl.query.service.IFewshotExampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Few-shot 示例管理 Controller
 */
@RestController
@RequestMapping("/api/metadata/fewshot-examples")
public class FewshotExampleController extends BaseController
{
    @Autowired
    private IFewshotExampleService fewshotExampleService;

    @PreAuthorize("@ss.hasPermi('metadata:fewshot:list')")
    @GetMapping("/list")
    public TableDataInfo list(FewshotExample query)
    {
        startPage();
        List<FewshotExample> list = fewshotExampleService.selectList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('metadata:fewshot:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(fewshotExampleService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('metadata:fewshot:add')")
    @PostMapping
    public AjaxResult add(@RequestBody FewshotExample example)
    {
        example.setCreatedBy(SecurityUtils.getUserId());
        return toAjax(fewshotExampleService.create(example));
    }

    @PreAuthorize("@ss.hasPermi('metadata:fewshot:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody FewshotExample example)
    {
        return toAjax(fewshotExampleService.update(example));
    }

    @PreAuthorize("@ss.hasPermi('metadata:fewshot:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        return toAjax(fewshotExampleService.delete(id));
    }

    @PreAuthorize("@ss.hasPermi('metadata:fewshot:edit')")
    @PutMapping("/enabled")
    public AjaxResult updateEnabled(@RequestBody Map<String, Object> params)
    {
        Long id = Long.valueOf(params.get("id").toString());
        Integer enabled = Integer.valueOf(params.get("enabled").toString());
        return toAjax(fewshotExampleService.updateEnabled(id, enabled));
    }

    @PreAuthorize("@ss.hasPermi('metadata:fewshot:edit')")
    @PutMapping("/batch-enabled")
    public AjaxResult batchUpdateEnabled(@RequestBody Map<String, Object> params)
    {
        @SuppressWarnings("unchecked")
        List<Number> idNums = (List<Number>) params.get("ids");
        List<Long> ids = idNums.stream().map(Number::longValue).toList();
        Integer enabled = Integer.valueOf(params.get("enabled").toString());
        return toAjax(fewshotExampleService.batchUpdateEnabled(ids, enabled));
    }

    @PreAuthorize("@ss.hasPermi('metadata:fewshot:import')")
    @PostMapping("/import-from-feedback")
    public AjaxResult importFromFeedback(@RequestBody Map<String, Object> params)
    {
        Long feedbackId = Long.valueOf(params.get("feedbackId").toString());
        Long userId = SecurityUtils.getUserId();
        return toAjax(fewshotExampleService.importFromFeedback(feedbackId, userId));
    }
}
