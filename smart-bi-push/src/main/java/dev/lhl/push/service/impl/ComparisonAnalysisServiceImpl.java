package dev.lhl.push.service.impl;

import dev.lhl.push.domain.BiSubscription;
import dev.lhl.push.service.IComparisonAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 对比分析服务实现
 * 占位：实际实现需根据 subscribeType+targetId 拉取当前/上期/同期数据并计算
 *
 * @author smart-bi
 */
@Service
public class ComparisonAnalysisServiceImpl implements IComparisonAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ComparisonAnalysisServiceImpl.class);

    @Override
    public Map<String, Object> generateComparisonSummary(BiSubscription subscription) {
        if (subscription == null) return new HashMap<>();
        Map<String, Object> summary = new HashMap<>();
        try {
            // 占位：实际需查询目标数据，计算环比、同比，生成摘要
            // 例：periodOverPeriod: "较上期 +12.5%", yearOverYear: "较去年同期 -3.2%", highlights: [...]
            summary.put("periodOverPeriod", "（占位：与上期对比）");
            summary.put("yearOverYear", "（占位：与同期对比）");
            summary.put("highlights", java.util.Collections.emptyList());
            log.debug("生成对比分析: subscriptionId={}", subscription.getId());
        } catch (Exception e) {
            log.warn("对比分析生成失败: subscriptionId={}", subscription.getId(), e);
        }
        return summary;
    }
}
