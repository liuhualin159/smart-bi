package dev.lhl.push.service;

import dev.lhl.push.domain.BiSubscription;

import java.util.List;
import java.util.Map;

/**
 * 趋势预警服务：检测趋势变化并生成预警信息
 *
 * @author smart-bi
 */
public interface ITrendAlertService {

    /**
     * 检测订阅目标的趋势变化（如环比/同比显著升降）
     *
     * @param subscription 订阅配置
     * @return 若有预警返回列表，否则空列表
     */
    List<Map<String, Object>> detectTrendAlerts(BiSubscription subscription);
}
