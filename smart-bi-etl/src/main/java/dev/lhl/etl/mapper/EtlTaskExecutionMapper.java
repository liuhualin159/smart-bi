package dev.lhl.etl.mapper;

import dev.lhl.etl.domain.EtlTaskExecution;
import java.util.List;

/**
 * ETL任务执行记录Mapper接口
 * 
 * @author smart-bi
 */
public interface EtlTaskExecutionMapper
{
    /**
     * 查询ETL任务执行记录
     * 
     * @param id 执行记录ID
     * @return 执行记录
     */
    public EtlTaskExecution selectEtlTaskExecutionById(Long id);

    /**
     * 查询ETL任务执行记录列表
     * 
     * @param etlTaskExecution 执行记录
     * @return 执行记录集合
     */
    public List<EtlTaskExecution> selectEtlTaskExecutionList(EtlTaskExecution etlTaskExecution);

    /**
     * 根据任务ID查询执行记录列表
     * 
     * @param taskId 任务ID
     * @return 执行记录集合
     */
    public List<EtlTaskExecution> selectEtlTaskExecutionListByTaskId(Long taskId);

    /**
     * 新增ETL任务执行记录
     * 
     * @param etlTaskExecution 执行记录
     * @return 结果
     */
    public int insertEtlTaskExecution(EtlTaskExecution etlTaskExecution);

    /**
     * 修改ETL任务执行记录
     * 
     * @param etlTaskExecution 执行记录
     * @return 结果
     */
    public int updateEtlTaskExecution(EtlTaskExecution etlTaskExecution);

    /**
     * 删除ETL任务执行记录
     * 
     * @param id 执行记录ID
     * @return 结果
     */
    public int deleteEtlTaskExecutionById(Long id);

    /**
     * 批量删除ETL任务执行记录
     * 
     * @param ids 需要删除的执行记录ID
     * @return 结果
     */
    public int deleteEtlTaskExecutionByIds(Long[] ids);
    
    /**
     * 查询指定时间之后的执行记录
     * 
     * @param startTime 开始时间
     * @return 执行记录集合
     */
    public List<EtlTaskExecution> selectRecentExecutions(java.util.Date startTime);
    
    /**
     * 查询所有执行记录（用于监控）
     * 
     * @return 执行记录集合
     */
    public List<EtlTaskExecution> selectAllExecutions();
}
