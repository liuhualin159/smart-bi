package dev.lhl.push.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 推送记录对象 bi_push_record
 *
 * @author smart-bi
 */
public class BiPushRecord {

    private Long id;
    private Long subscriptionId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date pushAt;
    private String status;
    private Integer retryCount;
    private Date createTime;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }
    public Date getPushAt() { return pushAt; }
    public void setPushAt(Date pushAt) { this.pushAt = pushAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
