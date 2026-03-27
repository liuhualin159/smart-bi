package dev.lhl.push.service.impl;

import dev.lhl.push.domain.BiSubscription;
import dev.lhl.push.service.ITrendAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 趋势预警服务实现
 * 占位：实际实现需拉取多期数据，计算环比/同比变化率，超阈值时预警
 *
 * @author smart-bi
 */
@Service
public class TrendAlertServiceImpl implements ITrendAlertService {

    private static final Logger log = LoggerFactory.getLogger(TrendAlertServiceImpl.class);

    @Override
    public List<Map<String, Object>> detectTrendAlerts(BiSubscription subscription) {
        if (subscription == null) return new ArrayList<>();
        try {
            // 占位：实际需拉取当前/上期/同期数据，计算变化率，超阈值则加入预警
            log.debug("趋势预警检测: subscriptionId={}", subscription.getId());
            return new ArrayList<>();
        } catch (Exception e) {
            log.warn("趋势预警检测失败: subscriptionId={}", subscription.getId(), e);
            return new ArrayList<>();
        }
    }
}
