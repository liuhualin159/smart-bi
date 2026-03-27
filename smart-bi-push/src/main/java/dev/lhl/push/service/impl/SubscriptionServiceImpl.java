package dev.lhl.push.service.impl;

import dev.lhl.common.utils.SecurityUtils;
import dev.lhl.push.domain.BiSubscription;
import dev.lhl.push.domain.BiPushRecord;
import dev.lhl.push.mapper.BiSubscriptionMapper;
import dev.lhl.push.mapper.BiPushRecordMapper;
import dev.lhl.push.service.ISubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 订阅 Service 实现
 *
 * @author smart-bi
 */
@Service
public class SubscriptionServiceImpl implements ISubscriptionService {

    @Autowired
    private BiSubscriptionMapper biSubscriptionMapper;

    @Autowired
    private BiPushRecordMapper biPushRecordMapper;

    @Override
    public BiSubscription selectById(Long id) {
        return biSubscriptionMapper.selectById(id);
    }

    @Override
    public List<BiSubscription> selectList(BiSubscription query) {
        try {
            if (query.getUserId() == null) query.setUserId(SecurityUtils.getUserId());
        } catch (Exception ignored) {}
        return biSubscriptionMapper.selectList(query);
    }

    @Override
    public int insert(BiSubscription record) {
        if (record.getStatus() == null) record.setStatus("ENABLED");
        record.setCreateTime(new Date());
        try {
            record.setCreateBy(SecurityUtils.getUsername());
            if (record.getUserId() == null) record.setUserId(SecurityUtils.getUserId());
        } catch (Exception ignored) {}
        return biSubscriptionMapper.insert(record);
    }

    @Override
    public int updateById(BiSubscription record) {
        record.setUpdateTime(new Date());
        try { record.setUpdateBy(SecurityUtils.getUsername()); } catch (Exception ignored) {}
        return biSubscriptionMapper.updateById(record);
    }

    @Override
    public int deleteById(Long id) {
        return biSubscriptionMapper.deleteById(id);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return biSubscriptionMapper.deleteByIds(ids);
    }

    @Override
    public List<BiPushRecord> selectPushRecords(Long subscriptionId, Integer limit) {
        return biPushRecordMapper.selectBySubscriptionId(subscriptionId, limit != null ? limit : 50);
    }
}
