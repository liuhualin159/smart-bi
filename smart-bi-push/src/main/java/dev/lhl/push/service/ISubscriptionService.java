package dev.lhl.push.service;

import dev.lhl.push.domain.BiSubscription;
import dev.lhl.push.domain.BiPushRecord;

import java.util.List;

/**
 * 订阅 Service
 *
 * @author smart-bi
 */
public interface ISubscriptionService {

    BiSubscription selectById(Long id);

    List<BiSubscription> selectList(BiSubscription query);

    int insert(BiSubscription record);

    int updateById(BiSubscription record);

    int deleteById(Long id);

    int deleteByIds(Long[] ids);

    List<BiPushRecord> selectPushRecords(Long subscriptionId, Integer limit);
}
