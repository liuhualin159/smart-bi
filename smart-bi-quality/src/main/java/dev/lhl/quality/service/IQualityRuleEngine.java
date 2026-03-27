package dev.lhl.quality.service;

import dev.lhl.quality.domain.BiQualityRule;
import dev.lhl.quality.domain.RuleExecutionResult;

/**
 * 数据质量规则引擎
 * 按 rule_type 执行：COMPLETENESS/ACCURACY/CONSISTENCY/UNIQUENESS/TIMELINESS
 *
 * @author smart-bi
 */
public interface IQualityRuleEngine {

    /**
     * 执行单条规则
     *
     * @param rule      规则配置
     * @param tableName 表名（来自 bi_table_metadata）
     * @return 执行结果
     */
    RuleExecutionResult executeRule(BiQualityRule rule, String tableName);
}
