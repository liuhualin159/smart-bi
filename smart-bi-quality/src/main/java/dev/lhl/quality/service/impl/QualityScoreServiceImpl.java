package dev.lhl.quality.service.impl;

import dev.lhl.common.utils.SecurityUtils;
import dev.lhl.metadata.service.IMetadataService;
import dev.lhl.metadata.domain.TableMetadata;
import dev.lhl.quality.domain.BiQualityRule;
import dev.lhl.quality.domain.BiQualityScore;
import dev.lhl.quality.domain.RuleExecutionResult;
import dev.lhl.quality.mapper.BiQualityRuleMapper;
import dev.lhl.quality.mapper.BiQualityScoreMapper;
import dev.lhl.quality.service.IQualityRuleEngine;
import dev.lhl.quality.service.IQualityScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 质量评分：基于规则执行结果，按严重性权重计算 0-100 分
 *
 * @author smart-bi
 */
@Service
public class QualityScoreServiceImpl implements IQualityScoreService {

    @Autowired
    private BiQualityRuleMapper biQualityRuleMapper;

    @Autowired
    private BiQualityScoreMapper biQualityScoreMapper;

    @Autowired
    private IQualityRuleEngine qualityRuleEngine;

    @Autowired(required = false)
    private IMetadataService metadataService;

    @Override
    public BiQualityScore calculateAndSaveTableScore(Long tableId) {
        if (metadataService == null) return null;

        TableMetadata table = metadataService.selectTableMetadataById(tableId);
        if (table == null) return null;

        BiQualityRule query = new BiQualityRule();
        query.setTableId(tableId);
        query.setStatus("0");
        List<BiQualityRule> rules = biQualityRuleMapper.selectList(query);
        if (rules.isEmpty()) {
            BiQualityScore score = new BiQualityScore();
            score.setTableId(tableId);
            score.setScore(100);
            score.setScoreType("TABLE");
            score.setCalculatedAt(new Date());
            try { score.setCreateBy(SecurityUtils.getUsername()); } catch (Exception ignored) {}
            score.setCreateTime(new Date());
            score.setRemark("无规则，默认满分");
            biQualityScoreMapper.insert(score);
            return score;
        }

        int totalWeight = 0;
        int earnedWeight = 0;
        for (BiQualityRule rule : rules) {
            int w = rule.getSeverityWeight() != null ? rule.getSeverityWeight() : 1;
            totalWeight += w;
            RuleExecutionResult res = qualityRuleEngine.executeRule(rule, table.getTableName());
            if (res.isPassed()) {
                earnedWeight += w;
            } else {
                long total = res.getTotalRows();
                long failed = res.getFailedRows();
                if (total > 0) {
                    double passRate = 1.0 - (double) failed / total;
                    earnedWeight += (int) Math.round(w * passRate);
                }
            }
        }

        int scoreVal = totalWeight > 0 ? Math.min(100, (earnedWeight * 100) / totalWeight) : 100;

        BiQualityScore score = new BiQualityScore();
        score.setTableId(tableId);
        score.setScore(scoreVal);
        score.setScoreType("TABLE");
        score.setCalculatedAt(new Date());
        try { score.setCreateBy(SecurityUtils.getUsername()); } catch (Exception ignored) {}
        score.setCreateTime(new Date());
        biQualityScoreMapper.insert(score);
        return score;
    }

    @Override
    public List<BiQualityScore> getScoreHistory(Long tableId) {
        return biQualityScoreMapper.selectByTableId(tableId);
    }

    @Override
    public BiQualityScore getLatestTableScore(Long tableId) {
        return tableId == null ? null : biQualityScoreMapper.selectLatestByTableAndType(tableId, "TABLE");
    }
}
