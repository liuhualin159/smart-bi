package dev.lhl.push.domain;

import dev.lhl.common.core.domain.BaseEntity;

/**
 * 报表订阅对象 bi_subscription
 *
 * @author smart-bi
 */
public class BiSubscription extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String subscribeType;
    private Long targetId;
    private String scheduleCron;
    private String receiveChannels;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSubscribeType() { return subscribeType; }
    public void setSubscribeType(String subscribeType) { this.subscribeType = subscribeType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getScheduleCron() { return scheduleCron; }
    public void setScheduleCron(String scheduleCron) { this.scheduleCron = scheduleCron; }
    public String getReceiveChannels() { return receiveChannels; }
    public void setReceiveChannels(String receiveChannels) { this.receiveChannels = receiveChannels; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
