package dev.lhl.etl.service;

import dev.lhl.etl.domain.EtlTask;
import dev.lhl.etl.domain.EtlTaskExecution;
import java.util.List;

/**
 * ETL任务Service接口
 * 
 * @author smart-bi
 */
public interface IEtlTaskService
{
    /**
     * 查询ETL任务
     * 
     * @param id 任务ID
     * @return ETL任务
     */
    public EtlTask selectEtlTaskById(Long id);

    /**
     * 查询ETL任务列表
     * 
     * @param etlTask ETL任务
     * @return ETL任务集合
     */
    public List<EtlTask> selectEtlTaskList(EtlTask etlTask);

    /**
     * 新增ETL任务
     * 
     * @param etlTask ETL任务
     * @return 结果
     */
    public int insertEtlTask(EtlTask etlTask);

    /**
     * 修改ETL任务
     * 
     * @param etlTask ETL任务
     * @return 结果
     */
    public int updateEtlTask(EtlTask etlTask);

    /**
     * 批量删除ETL任务
     * 
     * @param ids 需要删除的任务ID
     * @return 结果
     */
    public int deleteEtlTaskByIds(Long[] ids);

    /**
     * 删除ETL任务信息
     * 
     * @param id 任务ID
     * @return 结果
     */
    public int deleteEtlTaskById(Long id);

    /**
     * 手动触发ETL任务执行
     * 
     * @param id 任务ID
     * @return 执行记录ID
     */
    public Long triggerEtlTask(Long id);

    /**
     * 暂停ETL任务
     * 
     * @param id 任务ID
     * @return 结果
     */
    public int pauseEtlTask(Long id);

    /**
     * 恢复ETL任务
     * 
     * @param id 任务ID
     * @return 结果
     */
    public int resumeEtlTask(Long id);

    /**
     * 执行ETL任务
     * 
     * @param taskId 任务ID
     * @return 执行记录
     */
    public EtlTaskExecution executeEtlTask(Long taskId);

    /**
     * 查询ETL任务执行记录列表
     * 
     * @param taskId 任务ID
     * @return 执行记录集合
     */
    public List<EtlTaskExecution> selectEtlTaskExecutionListByTaskId(Long taskId);
}
