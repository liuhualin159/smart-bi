package dev.lhl.push.service;

import dev.lhl.push.domain.BiSubscription;

/**
 * 推送执行服务：按订阅配置生成内容并推送
 *
 * @author smart-bi
 */
public interface IPushExecutionService {

    /**
     * 执行单次推送
     *
     * @param subscription 订阅配置
     * @return 是否成功
     */
    boolean executePush(BiSubscription subscription);

    /**
     * 执行推送（含重试，最多3次）
     *
     * @param subscription 订阅配置
     */
    void executePushWithRetry(BiSubscription subscription);
}
