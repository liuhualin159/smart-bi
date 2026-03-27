package dev.lhl.push.job;

import dev.lhl.push.domain.BiSubscription;
import dev.lhl.push.mapper.BiPushRecordMapper;
import dev.lhl.push.mapper.BiSubscriptionMapper;
import dev.lhl.push.service.IPushExecutionService;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 报表订阅推送调度器
 * 由 Quartz 每分钟调用，扫描到期的 ENABLED 订阅并执行推送
 *
 * @author smart-bi
 */
@Component("subscriptionPushRunner")
public class SubscriptionPushRunner {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionPushRunner.class);

    @Autowired
    private BiSubscriptionMapper subscriptionMapper;

    @Autowired
    private BiPushRecordMapper pushRecordMapper;

    @Autowired
    private IPushExecutionService pushExecutionService;

    /**
     * 由 Quartz 调用：扫描并执行到期的订阅
     */
    public void execute() {
        try {
            BiSubscription query = new BiSubscription();
            query.setStatus("ENABLED");
            List<BiSubscription> list = subscriptionMapper.selectList(query);
            Date now = new Date();
            for (BiSubscription sub : list) {
                if (isDue(sub, now)) {
                    try {
                        pushExecutionService.executePushWithRetry(sub);
                    } catch (Exception e) {
                        log.error("订阅推送异常: subscriptionId={}", sub.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("订阅推送调度执行异常", e);
        }
    }

    private boolean isDue(BiSubscription sub, Date now) {
        if (sub.getScheduleCron() == null || sub.getScheduleCron().isEmpty()) return false;
        try {
            CronExpression cron = new CronExpression(sub.getScheduleCron());
            Date base = pushRecordMapper.selectLastPushAt(sub.getId());
            if (base == null) base = sub.getCreateTime() != null ? sub.getCreateTime() : new Date(0);
            Date next = cron.getNextValidTimeAfter(base);
            return next != null && !next.after(now);
        } catch (Exception e) {
            log.warn("解析 cron 失败: subscriptionId={}, cron={}", sub.getId(), sub.getScheduleCron(), e);
            return false;
        }
    }
}
