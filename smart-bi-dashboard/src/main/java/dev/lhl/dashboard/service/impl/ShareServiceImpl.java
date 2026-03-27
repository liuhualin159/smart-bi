package dev.lhl.dashboard.service.impl;

import dev.lhl.dashboard.domain.ShareLink;
import dev.lhl.dashboard.domain.Dashboard;
import dev.lhl.dashboard.domain.ChartCard;
import dev.lhl.dashboard.mapper.ShareLinkMapper;
import dev.lhl.dashboard.service.IDashboardService;
import dev.lhl.dashboard.service.IChartCardService;
import dev.lhl.dashboard.service.IShareService;
import dev.lhl.common.utils.StringUtils;
import dev.lhl.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 分享服务实现
 * 负责生成和管理分享链接
 * 
 * @author smart-bi
 */
@Service
public class ShareServiceImpl implements IShareService
{
    private static final Logger log = LoggerFactory.getLogger(ShareServiceImpl.class);
    
    @Value("${share.base-url:http://localhost}")
    private String shareBaseUrl;
    
    @Autowired
    private ShareLinkMapper shareLinkMapper;
    
    @Autowired
    private IDashboardService dashboardService;
    
    @Autowired
    private IChartCardService chartCardService;
    
    @Override
    public ShareLinkInfo generateShareLink(String resourceType, Long resourceId, Long userId, 
                                            String password, Integer expireDays, Long maxAccessCount)
    {
        try
        {
            log.info("生成分享链接: resourceType={}, resourceId={}, userId={}", resourceType, resourceId, userId);
            
            // 1. 验证资源是否存在
            if (!validateResource(resourceType, resourceId))
            {
                throw new RuntimeException("资源不存在: resourceType=" + resourceType + ", resourceId=" + resourceId);
            }
            
            // 2. 生成分享密钥（唯一标识）
            String shareKey = generateShareKey();
            
            // 3. 计算过期时间
            if (expireDays == null || expireDays <= 0)
            {
                expireDays = 7; // 默认7天
            }
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.add(java.util.Calendar.DAY_OF_MONTH, expireDays);
            java.util.Date expireTime = calendar.getTime();
            
            // 4. 加密密码（如果提供）
            String encryptedPassword = null;
            if (StringUtils.isNotEmpty(password))
            {
                encryptedPassword = dev.lhl.common.utils.EncryptUtils.encrypt(password);
            }
            
            // 5. 创建分享链接记录
            ShareLink shareLink = new ShareLink();
            shareLink.setShareKey(shareKey);
            shareLink.setResourceType(resourceType);
            shareLink.setResourceId(resourceId);
            shareLink.setUserId(userId);
            shareLink.setPassword(encryptedPassword);
            shareLink.setExpireTime(expireTime);
            shareLink.setAccessCount(0L);
            shareLink.setMaxAccessCount(maxAccessCount != null && maxAccessCount > 0 ? maxAccessCount : null);
            shareLink.setStatus("ACTIVE");
            shareLink.setCreateBy(SecurityUtils.getUsername());
            shareLink.setCreateTime(new java.util.Date());
            
            shareLinkMapper.insertShareLink(shareLink);
            
            // 6. 构建分享URL（固定前端路径 /share/{shareKey}）
            String shareUrl = shareBaseUrl.replaceAll("/+$", "") + "/share/" + shareKey;
            
            log.info("分享链接生成成功: shareKey={}, shareUrl={}", shareKey, shareUrl);
            
            return new ShareLinkInfo(shareKey, shareUrl, expireTime);
        }
        catch (Exception e)
        {
            log.error("生成分享链接失败: resourceType={}, resourceId={}", resourceType, resourceId, e);
            throw new RuntimeException("生成分享链接失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, Object> accessShareLink(String shareKey, String password) throws ShareLinkException
    {
        try
        {
            log.info("访问分享链接: shareKey={}", shareKey);
            
            // 1. 查询分享链接
            ShareLink shareLink = shareLinkMapper.selectShareLinkByShareKey(shareKey);
            if (shareLink == null)
            {
                throw new ShareLinkException("LINK_NOT_FOUND", "分享链接不存在");
            }
            
            // 2. 检查状态
            if ("DISABLED".equals(shareLink.getStatus()))
            {
                throw new ShareLinkException("LINK_DISABLED", "分享链接已禁用");
            }
            
            // 3. 检查是否过期
            if (shareLink.getExpireTime() != null && shareLink.getExpireTime().before(new java.util.Date()))
            {
                shareLink.setStatus("EXPIRED");
                shareLinkMapper.updateShareLink(shareLink);
                throw new ShareLinkException("LINK_EXPIRED", "分享链接已过期");
            }
            
            // 4. 检查访问次数限制（maxAccessCount > 0 时生效）
            if (shareLink.getMaxAccessCount() != null && shareLink.getMaxAccessCount() > 0)
            {
                long current = shareLink.getAccessCount() != null ? shareLink.getAccessCount() : 0;
                if (current >= shareLink.getMaxAccessCount())
                {
                    throw new ShareLinkException("LINK_ACCESS_LIMIT", "分享链接访问次数已用完");
                }
            }
            
            // 5. 验证密码（如果有）
            if (StringUtils.isNotEmpty(shareLink.getPassword()))
            {
                if (StringUtils.isEmpty(password))
                {
                    throw new ShareLinkException("PASSWORD_REQUIRED", "需要访问密码");
                }
                
                String decryptedPassword = dev.lhl.common.utils.EncryptUtils.decrypt(shareLink.getPassword());
                if (!password.equals(decryptedPassword))
                {
                    throw new ShareLinkException("PASSWORD_INCORRECT", "访问密码错误");
                }
            }
            
            // 6. 获取资源数据
            Map<String, Object> resourceData = getResourceData(shareLink.getResourceType(), shareLink.getResourceId());
            if (resourceData == null)
            {
                throw new ShareLinkException("RESOURCE_DELETED", "资源已被删除");
            }
            
            // 7. 增加访问次数
            shareLinkMapper.incrementAccessCount(shareKey);
            
            // 8. 构建响应数据
            Map<String, Object> result = new HashMap<>();
            result.put("shareLink", shareLink);
            result.put("resource", resourceData);
            result.put("resourceType", shareLink.getResourceType());
            
            log.info("分享链接访问成功: shareKey={}, accessCount={}", shareKey, shareLink.getAccessCount() + 1);
            
            return result;
        }
        catch (ShareLinkException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("访问分享链接失败: shareKey={}", shareKey, e);
            throw new ShareLinkException("ACCESS_FAILED", "访问分享链接失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<ShareLink> selectShareLinkList(ShareLink shareLink)
    {
        return shareLinkMapper.selectShareLinkList(shareLink);
    }
    
    @Override
    public int disableShareLink(Long id)
    {
        try
        {
            ShareLink shareLink = shareLinkMapper.selectShareLinkById(id);
            if (shareLink == null)
            {
                return 0;
            }
            
            shareLink.setStatus("DISABLED");
            shareLink.setUpdateBy(SecurityUtils.getUsername());
            shareLink.setUpdateTime(new java.util.Date());
            
            return shareLinkMapper.updateShareLink(shareLink);
        }
        catch (Exception e)
        {
            log.error("禁用分享链接失败: id={}", id, e);
            return 0;
        }
    }
    
    @Override
    public int deleteShareLinkByIds(Long[] ids)
    {
        return shareLinkMapper.deleteShareLinkByIds(ids);
    }
    
    /**
     * 验证资源是否存在
     */
    private boolean validateResource(String resourceType, Long resourceId)
    {
        try
        {
            if ("DASHBOARD".equals(resourceType))
            {
                Dashboard dashboard = dashboardService.selectDashboardById(resourceId);
                return dashboard != null;
            }
            else if ("CARD".equals(resourceType))
            {
                ChartCard card = chartCardService.selectChartCardById(resourceId);
                return card != null;
            }
            return false;
        }
        catch (Exception e)
        {
            log.warn("验证资源失败: resourceType={}, resourceId={}", resourceType, resourceId, e);
            return false;
        }
    }
    
    /**
     * 获取资源数据
     */
    private Map<String, Object> getResourceData(String resourceType, Long resourceId)
    {
        try
        {
            if ("DASHBOARD".equals(resourceType))
            {
                Dashboard dashboard = dashboardService.selectDashboardById(resourceId);
                if (dashboard == null)
                {
                    return null;
                }
                
                Map<String, Object> data = new HashMap<>();
                data.put("id", dashboard.getId());
                data.put("name", dashboard.getName());
                data.put("layoutConfig", dashboard.getLayoutConfig());
                data.put("refreshInterval", dashboard.getRefreshInterval());
                data.put("isPublic", dashboard.getIsPublic());
                // 注意：分享链接访问时，不返回敏感信息
                return data;
            }
            else if ("CARD".equals(resourceType))
            {
                ChartCard card = chartCardService.selectChartCardById(resourceId);
                if (card == null)
                {
                    return null;
                }
                
                Map<String, Object> data = new HashMap<>();
                data.put("id", card.getId());
                data.put("name", card.getName());
                data.put("chartType", card.getChartType());
                data.put("chartConfig", card.getChartConfig());
                // 注意：分享链接访问时，不返回SQL等敏感信息
                return data;
            }
            return null;
        }
        catch (Exception e)
        {
            log.warn("获取资源数据失败: resourceType={}, resourceId={}", resourceType, resourceId, e);
            return null;
        }
    }
    
    /**
     * 生成分享密钥（唯一标识）
     */
    private String generateShareKey()
    {
        // 使用UUID + 时间戳生成唯一密钥
        String uuid = UUID.randomUUID().toString().replace("-", "");
        long timestamp = System.currentTimeMillis();
        return uuid + "_" + Long.toHexString(timestamp);
    }
}
