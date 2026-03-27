package dev.lhl.web.controller.query;

import dev.lhl.common.core.controller.BaseController;
import dev.lhl.common.core.domain.AjaxResult;
import dev.lhl.query.service.IFeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 反馈审核 API（数据管理员）
 * PUT /api/feedback/{id}/approve、PUT /api/feedback/{id}/reject
 *
 * @author smart-bi
 */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController extends BaseController {

    @Autowired(required = false)
    private IFeedbackService feedbackService;

    /**
     * 审核通过
     */
    @PreAuthorize("@ss.hasPermi('bi:feedback:approve')")
    @PutMapping("/{id}/approve")
    public AjaxResult approve(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, Object> body) {
        String comment = body != null && body.get("reviewComment") != null ? String.valueOf(body.get("reviewComment")) : null;
        return doReview(id, "APPROVED", comment);
    }

    /**
     * 审核拒绝
     */
    @PreAuthorize("@ss.hasPermi('bi:feedback:approve')")
    @PutMapping("/{id}/reject")
    public AjaxResult reject(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, Object> body) {
        String comment = body != null && body.get("reviewComment") != null ? String.valueOf(body.get("reviewComment")) : null;
        return doReview(id, "REJECTED", comment);
    }

    private AjaxResult doReview(Long id, String status, String reviewComment) {
        if (feedbackService == null) {
            return error("反馈服务未配置");
        }
        try {
            int r = feedbackService.reviewFeedback(id, status, reviewComment, dev.lhl.common.utils.SecurityUtils.getUsername());
            return toAjax(r);
        } catch (Exception e) {
            return error("审核失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }
}
