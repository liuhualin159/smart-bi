package dev.lhl.query.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import dev.lhl.common.core.domain.BaseEntity;

/**
 * 查询会话对象 bi_query_session
 * 
 * @author smart-bi
 */
public class QuerySession extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 会话ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 会话键 */
    private String sessionKey;

    /** 对话上下文（JSON格式） */
    private String context;

    /** 最后活跃时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date lastActiveTime;

    /** 过期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date expireTime;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getUserId() { return userId; }
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
    public String getSessionKey() { return sessionKey; }
    public void setContext(String context) { this.context = context; }
    public String getContext() { return context; }
    public void setLastActiveTime(java.util.Date lastActiveTime) { this.lastActiveTime = lastActiveTime; }
    public java.util.Date getLastActiveTime() { return lastActiveTime; }
    public void setExpireTime(java.util.Date expireTime) { this.expireTime = expireTime; }
    public java.util.Date getExpireTime() { return expireTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("sessionKey", getSessionKey())
            .append("lastActiveTime", getLastActiveTime())
            .append("expireTime", getExpireTime())
            .toString();
    }
}
