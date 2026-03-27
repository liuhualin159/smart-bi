package dev.lhl.etl.mapper;

import dev.lhl.etl.domain.EtlTask;
import java.util.List;

/**
 * ETL任务Mapper接口
 * 
 * @author smart-bi
 */
public interface EtlTaskMapper
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
     * 删除ETL任务
     * 
     * @param id 任务ID
     * @return 结果
     */
    public int deleteEtlTaskById(Long id);

    /**
     * 批量删除ETL任务
     * 
     * @param ids 需要删除的任务ID
     * @return 结果
     */
    public int deleteEtlTaskByIds(Long[] ids);
}
