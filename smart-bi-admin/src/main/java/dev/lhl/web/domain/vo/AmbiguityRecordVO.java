package dev.lhl.web.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 歧义优化列表项 VO（来自 bi_llm_audit）
 *
 * @author smart-bi
 */
public class AmbiguityRecordVO
{
    private static final Pattern TABLE_PATTERN = Pattern.compile("(?:FROM|JOIN)\\s+[`]?(\\w+)[`]?", Pattern.CASE_INSENSITIVE);

    private Long id;
    /** 关联查询记录ID（用于「关联到问题」） */
    private Long queryId;
    /** 用户问题 */
    private String originalQuestion;
    /** 错误 SQL（可折叠展示） */
    private String generatedSql;
    /** 错误类型：WRONG_TABLE/WRONG_FIELD/WRONG_JOIN 等 */
    private String errorCategory;
    /** 处理状态：PENDING=未处理，RESOLVED=已处理 */
    private String processStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /** 涉及表名（从 SQL 解析，逗号分隔） */
    private String involvedTables;
    /** 涉及字段（从 SQL 解析，逗号分隔，可选） */
    private String involvedFields;
    private Long userId;

    /** 从 generatedSql 中解析表名，逗号分隔 */
    public static String parseInvolvedTables(String generatedSql)
    {
        if (generatedSql == null || generatedSql.isEmpty())
            return "";
        Set<String> tables = new LinkedHashSet<>();
        Matcher m = TABLE_PATTERN.matcher(generatedSql);
        while (m.find())
            tables.add(m.group(1));
        return String.join(", ", tables);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getQueryId() { return queryId; }
    public void setQueryId(Long queryId) { this.queryId = queryId; }
    public String getOriginalQuestion() { return originalQuestion; }
    public void setOriginalQuestion(String originalQuestion) { this.originalQuestion = originalQuestion; }
    public String getGeneratedSql() { return generatedSql; }
    public void setGeneratedSql(String generatedSql) { this.generatedSql = generatedSql; }
    public String getErrorCategory() { return errorCategory; }
    public void setErrorCategory(String errorCategory) { this.errorCategory = errorCategory; }
    public String getProcessStatus() { return processStatus; }
    public void setProcessStatus(String processStatus) { this.processStatus = processStatus; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getInvolvedTables() { return involvedTables; }
    public void setInvolvedTables(String involvedTables) { this.involvedTables = involvedTables; }
    public String getInvolvedFields() { return involvedFields; }
    public void setInvolvedFields(String involvedFields) { this.involvedFields = involvedFields; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
