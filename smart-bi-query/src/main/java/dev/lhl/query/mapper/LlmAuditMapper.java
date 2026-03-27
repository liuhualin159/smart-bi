package dev.lhl.query.mapper;

import dev.lhl.query.domain.LlmAudit;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * LLM审计Mapper接口
 * 
 * @author smart-bi
 */
public interface LlmAuditMapper
{
    /**
     * 查询LLM审计
     * 
     * @param id 审计ID
     * @return LLM审计
     */
    public LlmAudit selectLlmAuditById(Long id);

    /**
     * 查询LLM审计列表
     * 
     * @param llmAudit LLM审计
     * @return LLM审计集合
     */
    public List<LlmAudit> selectLlmAuditList(LlmAudit llmAudit);

    /**
     * 新增LLM审计
     * 
     * @param llmAudit LLM审计
     * @return 结果
     */
    public int insertLlmAudit(LlmAudit llmAudit);

    /**
     * 修改LLM审计
     * 
     * @param llmAudit LLM审计
     * @return 结果
     */
    public int updateLlmAudit(LlmAudit llmAudit);

    /**
     * 删除LLM审计
     * 
     * @param id 审计ID
     * @return 结果
     */
    public int deleteLlmAuditById(Long id);

    /**
     * 批量删除LLM审计
     * 
     * @param ids 需要删除的审计ID
     * @return 结果
     */
    public int deleteLlmAuditByIds(Long[] ids);

    /**
     * 查询时间窗口内有错误类型的审计记录（create_time >= startTime 且 error_category 非空）
     */
    List<LlmAudit> selectLlmAuditListSince(@Param("startTime") java.util.Date startTime);

    /**
     * 歧义优化列表：分页筛选（errorCategory、tableName、startTime、endTime、processStatus）
     * 仅返回有错误类型的记录（error_category 非空）
     */
    List<LlmAudit> selectLlmAuditListForAmbiguity(
        @Param("errorCategory") String errorCategory,
        @Param("tableName") String tableName,
        @Param("startTime") java.util.Date startTime,
        @Param("endTime") java.util.Date endTime,
        @Param("processStatus") String processStatus
    );

    /**
     * 歧义汇总用：时间范围内有错误类型的记录，最多 3000 条（用于按表聚合）
     */
    List<LlmAudit> selectLlmAuditListForAmbiguitySummary(
        @Param("startTime") java.util.Date startTime,
        @Param("endTime") java.util.Date endTime
    );
}
