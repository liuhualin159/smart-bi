package dev.lhl.push.service;

import dev.lhl.push.domain.BiSubscription;

import java.util.Map;

/**
 * 对比分析服务：与上期/同期对比，用于推送内容增强
 *
 * @author smart-bi
 */
public interface IComparisonAnalysisService {

    /**
     * 生成对比分析摘要（与上期、同期对比）
     *
     * @param subscription 订阅配置
     * @return 含 periodOverPeriod、yearOverYear 等键的 Map，可嵌入推送内容
     */
    Map<String, Object> generateComparisonSummary(BiSubscription subscription);
}
