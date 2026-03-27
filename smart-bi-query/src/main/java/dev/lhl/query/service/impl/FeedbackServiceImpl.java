package dev.lhl.query.service.impl;

import dev.lhl.query.domain.Feedback;
import dev.lhl.query.mapper.FeedbackMapper;
import dev.lhl.query.service.IFeedbackCorrectionService;
import dev.lhl.query.service.IFeedbackService;
import dev.lhl.common.utils.SecurityUtils;
import dev.lhl.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 反馈服务实现
 * 负责管理查询结果的反馈
 * 
 * @author smart-bi
 */
@Service
public class FeedbackServiceImpl implements IFeedbackService
{
    private static final Logger log = LoggerFactory.getLogger(FeedbackServiceImpl.class);
    
    @Autowired
    private FeedbackMapper feedbackMapper;

    @Autowired(required = false)
    private IFeedbackCorrectionService feedbackCorrectionService;
    
    @Override
    public int submitFeedback(Feedback feedback)
    {
        try
        {
            log.info("提交反馈: queryId={}, feedbackType={}, userId={}", 
                feedback.getQueryId(), feedback.getFeedbackType(), feedback.getUserId());
            
            // 设置默认值
            if (StringUtils.isEmpty(feedback.getReviewStatus()))
            {
                feedback.setReviewStatus("PENDING");
            }
            
            feedback.setCreateBy(SecurityUtils.getUsername());
            feedback.setCreateTime(new java.util.Date());
            
            return feedbackMapper.insertFeedback(feedback);
        }
        catch (Exception e)
        {
            log.error("提交反馈失败: queryId={}", feedback.getQueryId(), e);
            throw new RuntimeException("提交反馈失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<Feedback> selectFeedbackList(Feedback feedback)
    {
        return feedbackMapper.selectFeedbackList(feedback);
    }
    
    @Override
    public Feedback selectFeedbackById(Long id)
    {
        return feedbackMapper.selectFeedbackById(id);
    }
    
    @Override
    public int reviewFeedback(Long id, String reviewStatus, String reviewComment, String reviewer)
    {
        try
        {
            log.info("审核反馈: id={}, reviewStatus={}, reviewer={}", id, reviewStatus, reviewer);
            
            Feedback feedback = feedbackMapper.selectFeedbackById(id);
            if (feedback == null)
            {
                throw new RuntimeException("反馈不存在: id=" + id);
            }
            
            feedback.setReviewStatus(reviewStatus);
            feedback.setReviewComment(reviewComment);
            feedback.setReviewer(reviewer);
            feedback.setReviewTime(new java.util.Date());
            feedback.setUpdateBy(SecurityUtils.getUsername());
            feedback.setUpdateTime(new java.util.Date());
            
            int updated = feedbackMapper.updateFeedback(feedback);
            if (updated > 0 && "APPROVED".equals(reviewStatus) && feedbackCorrectionService != null)
            {
                try
                {
                    Long reviewerId = SecurityUtils.getUserId();
                    feedbackCorrectionService.createFromApprovedFeedback(id, reviewerId);
                }
                catch (Exception e) { log.warn("写入 feedback_correction 失败: feedbackId={}", id, e); }
            }
            return updated;
        }
        catch (Exception e)
        {
            log.error("审核反馈失败: id={}", id, e);
            throw new RuntimeException("审核反馈失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public int deleteFeedbackByIds(Long[] ids)
    {
        return feedbackMapper.deleteFeedbackByIds(ids);
    }
}
