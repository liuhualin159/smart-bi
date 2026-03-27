package dev.lhl.query.service;

import dev.lhl.query.domain.FeedbackCorrection;

import java.util.List;

/**
 * 反馈修正服务：审核通过写入 bi_feedback_correction，
 * 向量检索前按相似度阈值查询并注入 corrected_sql 到提示词
 *
 * @author smart-bi
 */
public interface IFeedbackCorrectionService {

    /**
     * 反馈审核通过时，写入 bi_feedback_correction
     *
     * @param feedbackId 反馈ID
     * @param reviewerId 审核人用户ID
     * @return 创建的 FeedbackCorrection 数量（0或1）
     */
    int createFromApprovedFeedback(Long feedbackId, Long reviewerId);

    /**
     * 按相似度阈值查询与当前问题相似的已审核修正记录
     *
     * @param question 用户问题
     * @param threshold 相似度阈值 0~1
     * @return 相似修正列表，按相似度降序
     */
    List<FeedbackCorrection> findSimilarCorrections(String question, double threshold);
}
