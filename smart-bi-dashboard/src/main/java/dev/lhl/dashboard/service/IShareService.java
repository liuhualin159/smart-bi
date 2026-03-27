package dev.lhl.dashboard.service;

import dev.lhl.dashboard.domain.ShareLink;
import java.util.Map;

/**
 * 分享服务接口
 * 负责生成和管理分享链接
 * 
 * @author smart-bi
 */
public interface IShareService
{
    /**
     * 生成分享链接
     * 
     * @param resourceType 资源类型（DASHBOARD/CARD）
     * @param resourceId 资源ID
     * @param userId 用户ID
     * @param password 访问密码（可选）
     * @param expireDays 过期天数（可选，默认7天）
     * @return 分享链接信息（包含shareKey和shareUrl）
     */
    ShareLinkInfo generateShareLink(String resourceType, Long resourceId, Long userId, String password, Integer expireDays, Long maxAccessCount);
    
    /**
     * 验证分享链接并获取资源
     * 
     * @param shareKey 分享密钥
     * @param password 访问密码（可选）
     * @return 资源数据（Dashboard或ChartCard）
     * @throws ShareLinkException 验证失败时抛出异常
     */
    Map<String, Object> accessShareLink(String shareKey, String password) throws ShareLinkException;
    
    /**
     * 获取分享链接列表
     * 
     * @param shareLink 查询条件
     * @return 分享链接列表
     */
    java.util.List<ShareLink> selectShareLinkList(ShareLink shareLink);
    
    /**
     * 禁用分享链接
     * 
     * @param id 分享链接ID
     * @return 是否成功
     */
    int disableShareLink(Long id);
    
    /**
     * 删除分享链接
     * 
     * @param ids 分享链接ID数组
     * @return 删除数量
     */
    int deleteShareLinkByIds(Long[] ids);
    
    /**
     * 分享链接信息
     */
    class ShareLinkInfo
    {
        private String shareKey;
        private String shareUrl;
        private java.util.Date expireTime;
        
        public ShareLinkInfo(String shareKey, String shareUrl, java.util.Date expireTime)
        {
            this.shareKey = shareKey;
            this.shareUrl = shareUrl;
            this.expireTime = expireTime;
        }
        
        public String getShareKey() { return shareKey; }
        public String getShareUrl() { return shareUrl; }
        public java.util.Date getExpireTime() { return expireTime; }
    }
    
    /**
     * 分享链接异常
     */
    class ShareLinkException extends Exception
    {
        private String errorCode;
        
        public ShareLinkException(String message)
        {
            super(message);
        }
        
        public ShareLinkException(String errorCode, String message)
        {
            super(message);
            this.errorCode = errorCode;
        }
        
        public String getErrorCode() { return errorCode; }
    }
}
