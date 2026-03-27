package dev.lhl.web.controller.etl;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import dev.lhl.common.annotation.Log;
import dev.lhl.common.core.controller.BaseController;
import dev.lhl.common.core.domain.AjaxResult;
import dev.lhl.common.core.page.TableDataInfo;
import dev.lhl.common.enums.BusinessType;
import dev.lhl.etl.domain.EtlTask;
import dev.lhl.etl.domain.EtlTaskExecution;
import dev.lhl.etl.service.IEtlTaskService;
import dev.lhl.etl.service.IEtlMonitorService;

/**
 * ETL任务Controller
 * 
 * @author smart-bi
 */
@RestController
@RequestMapping("/api/etl/task")
public class EtlTaskController extends BaseController
{
    @Autowired
    private IEtlTaskService etlTaskService;
    
    @Autowired
    private IEtlMonitorService etlMonitorService;

    /**
     * 查询ETL任务列表
     */
    @PreAuthorize("@ss.hasPermi('etl:task:list')")
    @GetMapping("/list")
    public TableDataInfo list(EtlTask etlTask)
    {
        startPage();
        List<EtlTask> list = etlTaskService.selectEtlTaskList(etlTask);
        return getDataTable(list);
    }

    /**
     * 获取ETL任务详细信息
     */
    @PreAuthorize("@ss.hasPermi('etl:task:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(etlTaskService.selectEtlTaskById(id));
    }

    /**
     * 新增ETL任务
     */
    @PreAuthorize("@ss.hasPermi('etl:task:add')")
    @Log(title = "ETL任务", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody EtlTask etlTask)
    {
        return toAjax(etlTaskService.insertEtlTask(etlTask));
    }

    /**
     * 修改ETL任务
     */
    @PreAuthorize("@ss.hasPermi('etl:task:edit')")
    @Log(title = "ETL任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody EtlTask etlTask)
    {
        return toAjax(etlTaskService.updateEtlTask(etlTask));
    }

    /**
     * 删除ETL任务
     */
    @PreAuthorize("@ss.hasPermi('etl:task:remove')")
    @Log(title = "ETL任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(etlTaskService.deleteEtlTaskByIds(ids));
    }

    /**
     * 手动触发ETL任务
     */
    @PreAuthorize("@ss.hasPermi('etl:task:trigger')")
    @Log(title = "ETL任务", businessType = BusinessType.UPDATE)
    @PostMapping("/trigger/{id}")
    public AjaxResult trigger(@PathVariable("id") Long id)
    {
        try
        {
            Long executionId = etlTaskService.triggerEtlTask(id);
            return AjaxResult.success("任务已触发执行", executionId);
        }
        catch (Exception e)
        {
            return error("触发任务失败: " + e.getMessage());
        }
    }

    /**
     * 暂停ETL任务
     */
    @PreAuthorize("@ss.hasPermi('etl:task:edit')")
    @Log(title = "ETL任务", businessType = BusinessType.UPDATE)
    @PostMapping("/pause/{id}")
    public AjaxResult pause(@PathVariable("id") Long id)
    {
        return toAjax(etlTaskService.pauseEtlTask(id));
    }

    /**
     * 恢复ETL任务
     */
    @PreAuthorize("@ss.hasPermi('etl:task:edit')")
    @Log(title = "ETL任务", businessType = BusinessType.UPDATE)
    @PostMapping("/resume/{id}")
    public AjaxResult resume(@PathVariable("id") Long id)
    {
        return toAjax(etlTaskService.resumeEtlTask(id));
    }

    /**
     * 查询ETL任务执行记录列表
     */
    @PreAuthorize("@ss.hasPermi('etl:task:list')")
    @GetMapping("/execution/{taskId}")
    public TableDataInfo getExecutionList(@PathVariable("taskId") Long taskId)
    {
        List<EtlTaskExecution> list = etlTaskService.selectEtlTaskExecutionListByTaskId(taskId);
        return getDataTable(list);
    }
    
    /**
     * 获取ETL任务执行记录列表（支持状态过滤）
     */
    @PreAuthorize("@ss.hasPermi('etl:task:list')")
    @GetMapping("/execution")
    public TableDataInfo getExecutionList(Long taskId, String status, Integer limit)
    {
        startPage();
        List<EtlTaskExecution> list = etlMonitorService.getExecutionList(taskId, status, limit);
        return getDataTable(list);
    }
    
    /**
     * 获取ETL任务监控数据
     */
    @PreAuthorize("@ss.hasPermi('etl:task:list')")
    @GetMapping("/monitor/data")
    public AjaxResult getMonitorData(Long taskId, Integer days)
    {
        try
        {
            return success(etlMonitorService.getMonitorData(taskId, days));
        }
        catch (Exception e)
        {
            return error("获取监控数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取ETL任务状态概览
     */
    @PreAuthorize("@ss.hasPermi('etl:task:list')")
    @GetMapping("/monitor/overview")
    public AjaxResult getTaskStatusOverview()
    {
        try
        {
            return success(etlMonitorService.getTaskStatusOverview());
        }
        catch (Exception e)
        {
            return error("获取任务状态概览失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定任务的执行状态
     */
    @PreAuthorize("@ss.hasPermi('etl:task:query')")
    @GetMapping("/monitor/status/{taskId}")
    public AjaxResult getTaskStatus(@PathVariable("taskId") Long taskId)
    {
        try
        {
            return success(etlMonitorService.getTaskStatus(taskId));
        }
        catch (Exception e)
        {
            return error("获取任务执行状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取任务执行趋势数据
     */
    @PreAuthorize("@ss.hasPermi('etl:task:list')")
    @GetMapping("/monitor/trend")
    public AjaxResult getExecutionTrend(Long taskId, Integer days)
    {
        try
        {
            return success(etlMonitorService.getExecutionTrend(taskId, days));
        }
        catch (Exception e)
        {
            return error("获取执行趋势数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取实时运行中的任务列表
     */
    @PreAuthorize("@ss.hasPermi('etl:task:list')")
    @GetMapping("/monitor/running")
    public AjaxResult getRunningTasks()
    {
        try
        {
            return success(etlMonitorService.getRunningTasks());
        }
        catch (Exception e)
        {
            return error("获取运行中的任务列表失败: " + e.getMessage());
        }
    }
}
