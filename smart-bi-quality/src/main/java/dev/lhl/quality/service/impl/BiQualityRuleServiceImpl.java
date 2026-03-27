package dev.lhl.quality.service.impl;

import dev.lhl.common.utils.SecurityUtils;
import dev.lhl.quality.domain.BiQualityRule;
import dev.lhl.quality.mapper.BiQualityRuleMapper;
import dev.lhl.quality.service.IBiQualityRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 数据质量规则 Service 实现
 *
 * @author smart-bi
 */
@Service
public class BiQualityRuleServiceImpl implements IBiQualityRuleService {

    @Autowired
    private BiQualityRuleMapper biQualityRuleMapper;

    @Override
    public BiQualityRule selectById(Long id) {
        return biQualityRuleMapper.selectById(id);
    }

    @Override
    public List<BiQualityRule> selectList(BiQualityRule query) {
        return biQualityRuleMapper.selectList(query);
    }

    @Override
    public int insert(BiQualityRule record) {
        if (record.getStatus() == null) record.setStatus("0");
        if (record.getPriority() == null) record.setPriority(0);
        if (record.getSeverityWeight() == null) record.setSeverityWeight(1);
        record.setCreateTime(new Date());
        try {
            record.setCreateBy(SecurityUtils.getUsername());
        } catch (Exception ignored) {}
        return biQualityRuleMapper.insert(record);
    }

    @Override
    public int updateById(BiQualityRule record) {
        record.setUpdateTime(new Date());
        try {
            record.setUpdateBy(SecurityUtils.getUsername());
        } catch (Exception ignored) {}
        return biQualityRuleMapper.updateById(record);
    }

    @Override
    public int deleteById(Long id) {
        return biQualityRuleMapper.deleteById(id);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return biQualityRuleMapper.deleteByIds(ids);
    }
}
