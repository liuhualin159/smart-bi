package dev.lhl.query.service;

import dev.lhl.query.domain.Feedback;
import java.util.List;

/**
 * 反馈服务接口
 * 负责管理查询结果的反馈
 * 
 * @author smart-bi
 */
public interface IFeedbackService
{
    /**
     * 提交反馈
     * 
     * @param feedback 反馈对象
     * @return 是否成功
     */
    int submitFeedback(Feedback feedback);
    
    /**
     * 获取反馈列表
     * 
     * @param feedback 查询条件
     * @return 反馈列表
     */
    List<Feedback> selectFeedbackList(Feedback feedback);
    
    /**
     * 获取反馈详情
     * 
     * @param id 反馈ID
     * @return 反馈对象
     */
    Feedback selectFeedbackById(Long id);
    
    /**
     * 审核反馈（管理员）
     * 
     * @param id 反馈ID
     * @param reviewStatus 审核状态（APPROVED/REJECTED）
     * @param reviewComment 审核意见
     * @param reviewer 审核人
     * @return 是否成功
     */
    int reviewFeedback(Long id, String reviewStatus, String reviewComment, String reviewer);
    
    /**
     * 删除反馈
     * 
     * @param ids 反馈ID数组
     * @return 删除数量
     */
    int deleteFeedbackByIds(Long[] ids);
}
