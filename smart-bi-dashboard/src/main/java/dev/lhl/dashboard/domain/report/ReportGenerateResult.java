package dev.lhl.dashboard.domain.report;

import java.util.List;
import java.util.Map;

/**
 * 报表生成结果：卡片列表 + 布局建议，与设计器 cards/layout 结构兼容
 */
public class ReportGenerateResult {

    /** 生成的卡片列表，每项为可写入画布的数据源卡片结构（含 datasourceConfig） */
    private List<Map<String, Object>> cards;

    /** 布局建议，每项含 cardKey（与 cards 下标或临时 id 对应）、x、y、w、h */
    private List<Map<String, Object>> layout;

    /** 错误码（整体失败时） */
    private String errorCode;

    /** 用户可读错误文案（整体失败时） */
    private String errorMessage;

    public List<Map<String, Object>> getCards() {
        return cards;
    }

    public void setCards(List<Map<String, Object>> cards) {
        this.cards = cards;
    }

    public List<Map<String, Object>> getLayout() {
        return layout;
    }

    public void setLayout(List<Map<String, Object>> layout) {
        this.layout = layout;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public static ReportGenerateResult success(List<Map<String, Object>> cards, List<Map<String, Object>> layout) {
        ReportGenerateResult r = new ReportGenerateResult();
        r.setCards(cards != null ? cards : List.of());
        r.setLayout(layout != null ? layout : List.of());
        return r;
    }

    public static ReportGenerateResult failure(String errorCode, String errorMessage) {
        ReportGenerateResult r = new ReportGenerateResult();
        r.setCards(List.of());
        r.setLayout(List.of());
        r.setErrorCode(errorCode);
        r.setErrorMessage(errorMessage);
        return r;
    }
}
