package dev.lhl.quality.service;

import dev.lhl.quality.domain.BiQualityRule;

import java.util.List;

/**
 * 数据质量规则 Service 接口
 *
 * @author smart-bi
 */
public interface IBiQualityRuleService {

    BiQualityRule selectById(Long id);

    List<BiQualityRule> selectList(BiQualityRule query);

    int insert(BiQualityRule record);

    int updateById(BiQualityRule record);

    int deleteById(Long id);

    int deleteByIds(Long[] ids);
}
