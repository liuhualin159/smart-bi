package dev.lhl.dashboard.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import dev.lhl.common.annotation.Excel;
import dev.lhl.common.core.domain.BaseEntity;

/**
 * 分享链接对象 bi_share_link
 * 
 * @author smart-bi
 */
public class ShareLink extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 分享链接ID */
    private Long id;

    /** 分享密钥（唯一标识） */
    @Excel(name = "分享密钥")
    private String shareKey;

    /** 资源类型（DASHBOARD/CARD） */
    @Excel(name = "资源类型", readConverterExp = "DASHBOARD=看板,CARD=卡片")
    private String resourceType;

    /** 资源ID */
    @Excel(name = "资源ID")
    private Long resourceId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 访问密码（可选，加密存储） */
    private String password;

    /** 过期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "过期时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date expireTime;

    /** 访问次数 */
    @Excel(name = "访问次数")
    private Long accessCount;

    /** 最大访问次数（null/0 表示不限制） */
    private Long maxAccessCount;

    /** 状态（ACTIVE/EXPIRED/DISABLED） */
    @Excel(name = "状态", readConverterExp = "ACTIVE=激活,EXPIRED=过期,DISABLED=禁用")
    private String status;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }
    
    public void setShareKey(String shareKey) { this.shareKey = shareKey; }
    public String getShareKey() { return shareKey; }
    
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceType() { return resourceType; }
    
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public Long getResourceId() { return resourceId; }
    
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getUserId() { return userId; }
    
    public void setPassword(String password) { this.password = password; }
    public String getPassword() { return password; }
    
    public void setExpireTime(java.util.Date expireTime) { this.expireTime = expireTime; }
    public java.util.Date getExpireTime() { return expireTime; }
    
    public void setAccessCount(Long accessCount) { this.accessCount = accessCount; }
    public Long getAccessCount() { return accessCount; }
    
    public void setMaxAccessCount(Long maxAccessCount) { this.maxAccessCount = maxAccessCount; }
    public Long getMaxAccessCount() { return maxAccessCount; }
    
    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("shareKey", getShareKey())
            .append("resourceType", getResourceType())
            .append("resourceId", getResourceId())
            .append("userId", getUserId())
            .append("expireTime", getExpireTime())
            .append("accessCount", getAccessCount())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
