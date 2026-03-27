package dev.lhl.query.mapper;

import dev.lhl.query.domain.Feedback;
import java.util.List;

/**
 * 查询反馈Mapper接口
 * 
 * @author smart-bi
 */
public interface FeedbackMapper
{
    Feedback selectFeedbackById(Long id);
    List<Feedback> selectFeedbackList(Feedback feedback);
    int insertFeedback(Feedback feedback);
    int updateFeedback(Feedback feedback);
    int deleteFeedbackById(Long id);
    int deleteFeedbackByIds(Long[] ids);
}
