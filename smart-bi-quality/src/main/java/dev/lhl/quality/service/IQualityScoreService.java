package dev.lhl.quality.service;

import dev.lhl.quality.domain.BiQualityScore;

import java.util.List;

/**
 * 质量评分服务：表级/字段级评分，含规则严重性、数据量、业务影响权重
 *
 * @author smart-bi
 */
public interface IQualityScoreService {

    /**
     * 计算并保存表级质量评分
     *
     * @param tableId 表ID
     * @return 计算后的评分记录
     */
    BiQualityScore calculateAndSaveTableScore(Long tableId);

    /**
     * 查询表的评分历史
     */
    List<BiQualityScore> getScoreHistory(Long tableId);

    /**
     * 获取表最新评分（用于查询结果展示）
     */
    BiQualityScore getLatestTableScore(Long tableId);
}
