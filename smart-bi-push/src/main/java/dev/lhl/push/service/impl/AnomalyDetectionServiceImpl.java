package dev.lhl.push.service.impl;

import dev.lhl.push.domain.BiSubscription;
import dev.lhl.push.service.IAnomalyDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 异常检测服务实现
 * 占位：实际实现需拉取目标数据，按 3σ、阈值等规则检测异常
 *
 * @author smart-bi
 */
@Service
public class AnomalyDetectionServiceImpl implements IAnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionServiceImpl.class);

    @Override
    public List<Map<String, Object>> detectAnomalies(BiSubscription subscription) {
        if (subscription == null) return new ArrayList<>();
        try {
            // 占位：实际需根据 subscribeType+targetId 拉取数据，计算均值/标准差，检测异常点
            log.debug("异常检测: subscriptionId={}", subscription.getId());
            return new ArrayList<>();
        } catch (Exception e) {
            log.warn("异常检测失败: subscriptionId={}", subscription.getId(), e);
            return new ArrayList<>();
        }
    }
}
