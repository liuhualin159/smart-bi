package dev.lhl.quality.service;

/**
 * 质量告警服务：评分/规则失败/质量下降时触发，多通道通知
 *
 * @author smart-bi
 */
public interface IQualityAlertService {

    /**
     * 检查并发送告警（可由定时任务调用）
     * 当表评分低于阈值、或规则大量失败时通知
     *
     * @param tableId 表ID，null 表示检查所有表
     * @param scoreThreshold 评分阈值，低于此值告警，默认 60
     * @return 发送的告警数量
     */
    int checkAndAlert(Long tableId, Integer scoreThreshold);
}
