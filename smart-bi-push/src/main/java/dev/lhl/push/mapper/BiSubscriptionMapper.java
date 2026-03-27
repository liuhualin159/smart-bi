package dev.lhl.push.mapper;

import dev.lhl.push.domain.BiSubscription;

import java.util.List;

/**
 * 订阅 Mapper
 *
 * @author smart-bi
 */
public interface BiSubscriptionMapper {

    BiSubscription selectById(Long id);

    List<BiSubscription> selectList(BiSubscription query);

    int insert(BiSubscription record);

    int updateById(BiSubscription record);

    int deleteById(Long id);

    int deleteByIds(Long[] ids);
}
