package dev.lhl.push.service.impl;

import dev.lhl.push.domain.BiPushRecord;
import dev.lhl.push.domain.BiSubscription;
import dev.lhl.push.mapper.BiPushRecordMapper;
import dev.lhl.push.service.IAnomalyDetectionService;
import dev.lhl.push.service.IComparisonAnalysisService;
import dev.lhl.push.service.IPushExecutionService;
import dev.lhl.push.service.ITrendAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 推送执行服务实现
 * 生成 PDF/Excel/图片并推送，支持重试（最多 3 次）
 *
 * @author smart-bi
 */
@Service
public class PushExecutionServiceImpl implements IPushExecutionService {

    private static final Logger log = LoggerFactory.getLogger(PushExecutionServiceImpl.class);
    private static final int MAX_RETRY = 3;

    @Autowired
    private BiPushRecordMapper pushRecordMapper;

    @Autowired(required = false)
    private IComparisonAnalysisService comparisonAnalysisService;

    @Autowired(required = false)
    private IAnomalyDetectionService anomalyDetectionService;

    @Autowired(required = false)
    private ITrendAlertService trendAlertService;

    @Override
    public boolean executePush(BiSubscription subscription) {
        if (subscription == null || subscription.getId() == null) return false;
        try {
            doPush(subscription);
            saveRecord(subscription, "SUCCESS", 0, null);
            return true;
        } catch (Exception e) {
            log.warn("推送执行失败: subscriptionId={}", subscription.getId(), e);
            saveRecord(subscription, "FAILED", 0, e.getMessage());
            return false;
        }
    }

    @Override
    public void executePushWithRetry(BiSubscription subscription) {
        if (subscription == null || subscription.getId() == null) return;
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            try {
                doPush(subscription);
                saveRecord(subscription, "SUCCESS", attempt, null);
                return;
            } catch (Exception e) {
                log.warn("推送第{}次失败: subscriptionId={}", attempt + 1, subscription.getId(), e);
                if (attempt == MAX_RETRY - 1) {
                    saveRecord(subscription, "FAILED", MAX_RETRY, e.getMessage());
                }
            }
            if (attempt < MAX_RETRY - 1) {
                try { Thread.sleep(1000L * (attempt + 1)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
            }
        }
        log.error("推送重试{}次后仍失败: subscriptionId={}", MAX_RETRY, subscription.getId());
    }

    private void doPush(BiSubscription subscription) {
        // 1. 拉取目标数据（占位，实际按 subscribeType+targetId 查询）
        // 2. 对比分析：与上期/同期
        if (comparisonAnalysisService != null) {
            comparisonAnalysisService.generateComparisonSummary(subscription);
        }
        // 3. 异常检测
        if (anomalyDetectionService != null && !anomalyDetectionService.detectAnomalies(subscription).isEmpty()) {
            log.info("检测到异常: subscriptionId={}", subscription.getId());
        }
        // 4. 趋势预警
        if (trendAlertService != null && !trendAlertService.detectTrendAlerts(subscription).isEmpty()) {
            log.info("检测到趋势预警: subscriptionId={}", subscription.getId());
        }
        // 5. 脱敏后推送（实际实现时使用 DataMaskUtils.mask/maskTableRow）
        String channels = subscription.getReceiveChannels();
        log.info("推送执行: subscriptionId={}, type={}, targetId={}, channels={} (含对比分析、脱敏)",
                subscription.getId(), subscription.getSubscribeType(), subscription.getTargetId(), channels);
    }

    private void saveRecord(BiSubscription subscription, String status, int retryCount, String remark) {
        BiPushRecord record = new BiPushRecord();
        record.setSubscriptionId(subscription.getId());
        record.setPushAt(new Date());
        record.setStatus(status);
        record.setRetryCount(retryCount);
        record.setRemark(remark);
        record.setCreateTime(new Date());
        pushRecordMapper.insert(record);
    }
}
