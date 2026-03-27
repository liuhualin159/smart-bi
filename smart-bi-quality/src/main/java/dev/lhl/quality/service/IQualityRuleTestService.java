package dev.lhl.quality.service;

import java.util.Map;

/**
 * 规则测试服务：从生产表抽样到临时区域执行规则，返回 passed/failed 统计，测试后清理
 *
 * @author smart-bi
 */
public interface IQualityRuleTestService {

    /**
     * 对指定表的规则进行抽样测试
     *
     * @param tableId    表ID (bi_table_metadata.id)
     * @param sampleSize 抽样行数，默认 1000
     * @return { totalRules, passed, failed, results: [ { ruleId, ruleType, passed, totalRows, failedRows, message } ] }
     */
    Map<String, Object> runRuleTest(Long tableId, int sampleSize);
}
