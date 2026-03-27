package dev.lhl.push.mapper;

import dev.lhl.push.domain.BiPushRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 推送记录 Mapper
 *
 * @author smart-bi
 */
public interface BiPushRecordMapper {

    int insert(BiPushRecord record);

    List<BiPushRecord> selectBySubscriptionId(@Param("subscriptionId") Long subscriptionId, @Param("limit") Integer limit);

    /** 获取最近一次推送时间（用于调度判断） */
    java.util.Date selectLastPushAt(@Param("subscriptionId") Long subscriptionId);
}
