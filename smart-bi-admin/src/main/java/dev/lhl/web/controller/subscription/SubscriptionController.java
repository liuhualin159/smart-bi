package dev.lhl.web.controller.subscription;

import dev.lhl.common.annotation.Log;
import dev.lhl.common.core.controller.BaseController;
import dev.lhl.common.core.domain.AjaxResult;
import dev.lhl.common.core.page.TableDataInfo;
import dev.lhl.common.enums.BusinessType;
import dev.lhl.push.domain.BiSubscription;
import dev.lhl.push.domain.BiPushRecord;
import dev.lhl.push.service.ISubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报表订阅 Controller
 *
 * @author smart-bi
 */
@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController extends BaseController {

    @Autowired
    private ISubscriptionService subscriptionService;

    @PreAuthorize("@ss.hasPermi('bi:subscription:list')")
    @GetMapping("/list")
    public TableDataInfo list(BiSubscription query) {
        startPage();
        List<BiSubscription> list = subscriptionService.selectList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('bi:subscription:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(subscriptionService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('bi:subscription:add')")
    @Log(title = "报表订阅", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BiSubscription record) {
        return toAjax(subscriptionService.insert(record));
    }

    @PreAuthorize("@ss.hasPermi('bi:subscription:edit')")
    @Log(title = "报表订阅", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BiSubscription record) {
        return toAjax(subscriptionService.updateById(record));
    }

    @PreAuthorize("@ss.hasPermi('bi:subscription:remove')")
    @Log(title = "报表订阅", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(subscriptionService.deleteByIds(ids));
    }

    /** 获取订阅的推送记录 */
    @PreAuthorize("@ss.hasPermi('bi:subscription:query')")
    @GetMapping("/{id}/pushRecords")
    public AjaxResult pushRecords(@PathVariable("id") Long id, Integer limit) {
        List<BiPushRecord> records = subscriptionService.selectPushRecords(id, limit);
        return success(records);
    }
}
