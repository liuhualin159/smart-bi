package dev.lhl.quality.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 数据质量评分对象 bi_quality_score
 *
 * @author smart-bi
 */
public class BiQualityScore {

    private Long id;
    private Long tableId;
    private Integer score;
    private String scoreType;
    private Long fieldId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date calculatedAt;
    private String createBy;
    private Date createTime;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getScoreType() { return scoreType; }
    public void setScoreType(String scoreType) { this.scoreType = scoreType; }
    public Long getFieldId() { return fieldId; }
    public void setFieldId(Long fieldId) { this.fieldId = fieldId; }
    public Date getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Date calculatedAt) { this.calculatedAt = calculatedAt; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
