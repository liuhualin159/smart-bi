package dev.lhl.web.controller.share;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import dev.lhl.common.annotation.Log;
import dev.lhl.common.core.controller.BaseController;
import dev.lhl.common.core.domain.AjaxResult;
import dev.lhl.common.core.page.TableDataInfo;
import dev.lhl.common.enums.BusinessType;
import dev.lhl.dashboard.domain.ShareLink;
import dev.lhl.dashboard.service.IShareService;
import dev.lhl.common.utils.SecurityUtils;

/**
 * 分享Controller
 * 负责生成和管理分享链接
 * 
 * @author smart-bi
 */
@RestController
@RequestMapping("/api/share")
public class ShareController extends BaseController
{
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ShareController.class);
    
    @Autowired
    private IShareService shareService;
    
    /**
     * 生成分享链接
     */
    @PreAuthorize("@ss.hasPermi('share:add')")
    @Log(title = "分享", businessType = BusinessType.INSERT)
    @PostMapping("/link")
    public AjaxResult generateShareLink(@RequestBody Map<String, Object> params)
    {
        try
        {
            String resourceType = (String) params.get("resourceType");
            Long resourceId = toLong(params.get("resourceId"));
            String password = (String) params.get("password");
            Integer expireDays = toInteger(params.get("expireDays"));
            Long maxAccessCount = toLong(params.get("maxAccessCount"));
            
            if (resourceType == null || resourceId == null)
            {
                return error("resourceType和resourceId不能为空");
            }
            
            Long userId = SecurityUtils.getUserId();
            
            IShareService.ShareLinkInfo shareLinkInfo = shareService.generateShareLink(
                resourceType, resourceId, userId, password, expireDays, maxAccessCount
            );
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("shareKey", shareLinkInfo.getShareKey());
            result.put("shareUrl", shareLinkInfo.getShareUrl());
            result.put("expireTime", shareLinkInfo.getExpireTime());
            
            return success(result);
        }
        catch (Exception e)
        {
            log.error("生成分享链接失败", e);
            return error("生成分享链接失败: " + e.getMessage());
        }
    }
    
    /**
     * 访问分享链接（无需登录）
     */
    @GetMapping("/{shareKey}")
    public AjaxResult accessShareLink(
        @PathVariable("shareKey") String shareKey,
        @RequestParam(required = false) String password)
    {
        try
        {
            log.info("访问分享链接: shareKey={}", shareKey);
            
            Map<String, Object> result = shareService.accessShareLink(shareKey, password);
            
            return success(result);
        }
        catch (IShareService.ShareLinkException e)
        {
            log.warn("分享链接访问失败: shareKey={}, errorCode={}, message={}", 
                shareKey, e.getErrorCode(), e.getMessage());
            
            String errorMessage = String.format("错误代码: %s, 错误信息: %s", e.getErrorCode(), e.getMessage());
            return error(errorMessage);
        }
        catch (Exception e)
        {
            log.error("访问分享链接失败: shareKey={}", shareKey, e);
            return error("访问分享链接失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取分享链接列表
     */
    @PreAuthorize("@ss.hasPermi('share:list')")
    @GetMapping("/list")
    public TableDataInfo getShareLinkList(ShareLink shareLink)
    {
        startPage();
        
        // 只查询当前用户的分享链接
        Long userId = SecurityUtils.getUserId();
        shareLink.setUserId(userId);
        
        List<ShareLink> list = shareService.selectShareLinkList(shareLink);
        return getDataTable(list);
    }
    
    /**
     * 禁用分享链接
     */
    @PreAuthorize("@ss.hasPermi('share:edit')")
    @Log(title = "分享", businessType = BusinessType.UPDATE)
    @PutMapping("/disable/{id}")
    public AjaxResult disableShareLink(@PathVariable("id") Long id)
    {
        try
        {
            int result = shareService.disableShareLink(id);
            return toAjax(result);
        }
        catch (Exception e)
        {
            log.error("禁用分享链接失败: id={}", id, e);
            return error("禁用分享链接失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除分享链接
     */
    @PreAuthorize("@ss.hasPermi('share:remove')")
    @Log(title = "分享", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult deleteShareLink(@PathVariable Long[] ids)
    {
        try
        {
            int result = shareService.deleteShareLinkByIds(ids);
            return toAjax(result);
        }
        catch (Exception e)
        {
            log.error("删除分享链接失败: ids={}", ids, e);
            return error("删除分享链接失败: " + e.getMessage());
        }
    }

    /** 安全解析为 Long（支持 String、Number） */
    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            String s = ((String) value).trim();
            return s.isEmpty() ? null : Long.parseLong(s);
        }
        return null;
    }

    /** 安全解析为 Integer（支持 String、Number） */
    private static Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            String s = ((String) value).trim();
            return s.isEmpty() ? null : Integer.parseInt(s);
        }
        return null;
    }
}
