package dev.lhl.dashboard.domain.report;

import java.util.Map;

/**
 * 报表生成进度回调，用于 SSE 流式推送
 */
public interface ReportGenerateProgressCallback {

    /** 推送一条进度/状态消息（展示在对话列表） */
    void onMessage(String message);

    /** 单张卡片已生成，可立即推送给前端并加入画布 */
    void onCardReady(int index, Map<String, Object> card, Map<String, Object> layoutItem);

    /** 全部完成 */
    void onComplete();

    /** 发生错误 */
    void onError(String code, String message);
}
