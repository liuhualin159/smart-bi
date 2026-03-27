package dev.lhl.web.controller.explore;

import dev.lhl.common.core.controller.BaseController;
import dev.lhl.common.core.domain.AjaxResult;
import dev.lhl.common.utils.SecurityUtils;
import dev.lhl.query.service.ITablePreviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据探索 Controller（表列表、预览、分析模板）
 *
 * @author smart-bi
 */
@RestController
@RequestMapping("/api/explore")
public class ExploreController extends BaseController {

    @Autowired
    private ITablePreviewService tablePreviewService;

    /**
     * 预览表数据
     *
     * @param tableId 表元数据ID
     * @param limit   最大行数，默认100
     */
    @PreAuthorize("@ss.hasPermi('metadata:table:list')")
    @GetMapping("/table/preview")
    public AjaxResult previewTable(
            @RequestParam("tableId") Long tableId,
            @RequestParam(value = "limit", defaultValue = "100") Integer limit) {
        Long userId = SecurityUtils.getUserId();
        ITablePreviewService.PreviewResult result = tablePreviewService.previewTable(tableId, limit, userId);
        Map<String, Object> data = new HashMap<>();
        data.put("columns", result.columns());
        data.put("data", result.data());
        return success(data);
    }
}
