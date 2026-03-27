package dev.lhl.quality.domain;

import dev.lhl.common.core.domain.BaseEntity;

/**
 * 数据质量规则对象 bi_quality_rule
 *
 * @author smart-bi
 */
public class BiQualityRule extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;
    /** 关联表ID */
    private Long tableId;
    /** 规则类型：COMPLETENESS/ACCURACY/CONSISTENCY/UNIQUENESS/TIMELINESS */
    private String ruleType;
    /** JSON配置(字段、阈值等) */
    private String ruleConfig;
    /** 优先级 */
    private Integer priority;
    /** 严重性权重(用于评分) */
    private Integer severityWeight;
    /** 状态(0正常 1停用) */
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public String getRuleConfig() { return ruleConfig; }
    public void setRuleConfig(String ruleConfig) { this.ruleConfig = ruleConfig; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Integer getSeverityWeight() { return severityWeight; }
    public void setSeverityWeight(Integer severityWeight) { this.severityWeight = severityWeight; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
