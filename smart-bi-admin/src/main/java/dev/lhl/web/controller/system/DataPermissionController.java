package dev.lhl.web.controller.system;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import dev.lhl.common.annotation.Log;
import dev.lhl.common.core.controller.BaseController;
import dev.lhl.common.core.domain.AjaxResult;
import dev.lhl.common.core.domain.DataPermission;
import dev.lhl.common.core.page.TableDataInfo;
import dev.lhl.common.enums.BusinessType;
import dev.lhl.system.service.IDataPermissionService;

/**
 * 数据权限Controller
 * 
 * @author smart-bi
 */
@RestController
@RequestMapping("/system/dataPermission")
public class DataPermissionController extends BaseController
{
    @Autowired
    private IDataPermissionService dataPermissionService;

    /**
     * 查询数据权限列表
     */
    @PreAuthorize("@ss.hasPermi('system:dataPermission:list')")
    @GetMapping("/list")
    public TableDataInfo list(DataPermission dataPermission)
    {
        startPage();
        List<DataPermission> list = dataPermissionService.selectDataPermissionList(dataPermission);
        return getDataTable(list);
    }

    /**
     * 获取数据权限详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:dataPermission:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(dataPermissionService.selectDataPermissionById(id));
    }

    /**
     * 新增数据权限
     */
    @PreAuthorize("@ss.hasPermi('system:dataPermission:add')")
    @Log(title = "数据权限", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DataPermission dataPermission)
    {
        return toAjax(dataPermissionService.insertDataPermission(dataPermission));
    }

    /**
     * 修改数据权限
     */
    @PreAuthorize("@ss.hasPermi('system:dataPermission:edit')")
    @Log(title = "数据权限", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DataPermission dataPermission)
    {
        return toAjax(dataPermissionService.updateDataPermission(dataPermission));
    }

    /**
     * 删除数据权限
     */
    @PreAuthorize("@ss.hasPermi('system:dataPermission:remove')")
    @Log(title = "数据权限", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(dataPermissionService.deleteDataPermissionByIds(ids));
    }

    /**
     * 检查表权限
     */
    @PreAuthorize("@ss.hasPermi('system:dataPermission:query')")
    @GetMapping("/checkTable/{userId}/{tableName}")
    public AjaxResult checkTablePermission(@PathVariable("userId") Long userId, @PathVariable("tableName") String tableName)
    {
        boolean hasPermission = dataPermissionService.checkTablePermission(userId, tableName);
        return success(hasPermission);
    }

    /**
     * 检查字段权限
     */
    @PreAuthorize("@ss.hasPermi('system:dataPermission:query')")
    @GetMapping("/checkField/{userId}/{tableName}/{fieldName}")
    public AjaxResult checkFieldPermission(@PathVariable("userId") Long userId, 
                                          @PathVariable("tableName") String tableName,
                                          @PathVariable("fieldName") String fieldName)
    {
        boolean hasPermission = dataPermissionService.checkFieldPermission(userId, tableName, fieldName);
        return success(hasPermission);
    }

    /**
     * 获取行级过滤条件
     */
    @PreAuthorize("@ss.hasPermi('system:dataPermission:query')")
    @GetMapping("/rowFilter/{userId}/{tableName}")
    public AjaxResult getRowFilter(@PathVariable("userId") Long userId, @PathVariable("tableName") String tableName)
    {
        String rowFilter = dataPermissionService.getRowFilter(userId, tableName);
        return success(rowFilter);
    }
}
