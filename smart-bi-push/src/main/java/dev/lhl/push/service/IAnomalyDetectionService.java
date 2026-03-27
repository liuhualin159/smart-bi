package dev.lhl.push.service;

import dev.lhl.push.domain.BiSubscription;

import java.util.List;
import java.util.Map;

/**
 * 异常检测服务：检测数据异常并触发推送
 *
 * @author smart-bi
 */
public interface IAnomalyDetectionService {

    /**
     * 检测订阅目标是否存在异常（如偏离基线、超阈值）
     *
     * @param subscription 订阅配置
     * @return 若有异常返回异常描述列表，否则空列表
     */
    List<Map<String, Object>> detectAnomalies(BiSubscription subscription);
}
