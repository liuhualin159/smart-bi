package dev.lhl.quality.domain;

import java.util.List;

/**
 * 规则执行结果
 *
 * @author smart-bi
 */
public class RuleExecutionResult {

    private Long ruleId;
    private String ruleType;
    private boolean passed;
    private long totalRows;
    private long failedRows;
    private String message;
    /** 失败样本（可选，最多 N 条） */
    private List<Object> failedSamples;

    public static RuleExecutionResult pass(Long ruleId, String ruleType, long totalRows) {
        RuleExecutionResult r = new RuleExecutionResult();
        r.setRuleId(ruleId);
        r.setRuleType(ruleType);
        r.setPassed(true);
        r.setTotalRows(totalRows);
        r.setFailedRows(0);
        r.setMessage("通过");
        return r;
    }

    public static RuleExecutionResult fail(Long ruleId, String ruleType, long totalRows, long failedRows, String message, List<Object> failedSamples) {
        RuleExecutionResult r = new RuleExecutionResult();
        r.setRuleId(ruleId);
        r.setRuleType(ruleType);
        r.setPassed(false);
        r.setTotalRows(totalRows);
        r.setFailedRows(failedRows);
        r.setMessage(message != null ? message : "未通过");
        r.setFailedSamples(failedSamples);
        return r;
    }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public long getTotalRows() { return totalRows; }
    public void setTotalRows(long totalRows) { this.totalRows = totalRows; }
    public long getFailedRows() { return failedRows; }
    public void setFailedRows(long failedRows) { this.failedRows = failedRows; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<Object> getFailedSamples() { return failedSamples; }
    public void setFailedSamples(List<Object> failedSamples) { this.failedSamples = failedSamples; }
}
