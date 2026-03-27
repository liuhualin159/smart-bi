package dev.lhl.web.controller.datasource;

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
import dev.lhl.datasource.domain.DataSource;
import dev.lhl.datasource.service.IDataSourceService;

/**
 * 数据源Controller
 * 
 * @author smart-bi
 */
@RestController
@RequestMapping("/api/datasource")
public class DataSourceController extends BaseController
{
    @Autowired
    private IDataSourceService dataSourceService;

    /**
     * 查询数据源列表（根路径，兼容带斜杠的请求）
     */
    @PreAuthorize("@ss.hasPermi('datasource:list')")
    @GetMapping({"", "/"})
    public TableDataInfo listRoot(DataSource dataSource)
    {
        return list(dataSource);
    }

    /**
     * 查询数据源列表
     */
    @PreAuthorize("@ss.hasPermi('datasource:list')")
    @GetMapping("/list")
    public TableDataInfo list(DataSource dataSource)
    {
        startPage();
        List<DataSource> list = dataSourceService.selectDataSourceList(dataSource);
        return getDataTable(list);
    }

    /**
     * 获取数据源详细信息
     */
    @PreAuthorize("@ss.hasPermi('datasource:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        DataSource dataSource = dataSourceService.selectDataSourceById(id);
        // 清除密码和认证配置（敏感信息），用户名保留以便编辑
        dataSource.setPassword(null);
        dataSource.setAuthConfig(null);
        return success(dataSource);
    }

    /**
     * 新增数据源
     */
    @PreAuthorize("@ss.hasPermi('datasource:add')")
    @Log(title = "数据源", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DataSource dataSource)
    {
        return toAjax(dataSourceService.insertDataSource(dataSource));
    }

    /**
     * 修改数据源
     */
    @PreAuthorize("@ss.hasPermi('datasource:edit')")
    @Log(title = "数据源", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DataSource dataSource)
    {
        return toAjax(dataSourceService.updateDataSource(dataSource));
    }

    /**
     * 删除数据源
     */
    @PreAuthorize("@ss.hasPermi('datasource:remove')")
    @Log(title = "数据源", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(dataSourceService.deleteDataSourceByIds(ids));
    }

    /**
     * 测试数据源连接
     * 支持两种方式：
     * 1. 传递数据源ID（通过路径参数或请求参数）
     * 2. 传递完整的数据源对象（通过请求体）
     */
    @PreAuthorize("@ss.hasPermi('datasource:test')")
    @PostMapping("/test")
    public AjaxResult testConnection(
        @RequestBody(required = false) DataSource dataSource,
        @org.springframework.web.bind.annotation.RequestParam(required = false) Long id)
    {
        try
        {
            // 如果传递了ID（来自请求参数或请求体）且未包含完整信息，则从数据库查询
            Long dataSourceId = (id != null) ? id : (dataSource != null ? dataSource.getId() : null);
            if (dataSourceId != null && (dataSource == null || dataSource.getName() == null || dataSource.getType() == null))
            {
                dataSource = dataSourceService.selectDataSourceById(dataSourceId);
                if (dataSource == null)
                {
                    return error("数据源不存在");
                }
            }
            
            // 如果既没有传递对象也没有传递ID，返回错误
            if (dataSource == null)
            {
                return error("请提供数据源信息或数据源ID");
            }
            
            boolean result = dataSourceService.testConnection(dataSource);
            if (result)
            {
                return success("连接测试成功");
            }
            else
            {
                return error("连接测试失败");
            }
        }
        catch (Exception e)
        {
            return error("连接测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试数据源连接（通过ID）
     */
    @PreAuthorize("@ss.hasPermi('datasource:test')")
    @PostMapping("/test/{id}")
    public AjaxResult testConnectionById(@PathVariable("id") Long id)
    {
        try
        {
            DataSource dataSource = dataSourceService.selectDataSourceById(id);
            if (dataSource == null)
            {
                return error("数据源不存在");
            }
            
            boolean result = dataSourceService.testConnection(dataSource);
            if (result)
            {
                return success("连接测试成功");
            }
            else
            {
                return error("连接测试失败");
            }
        }
        catch (Exception e)
        {
            return error("连接测试失败: " + e.getMessage());
        }
    }

    /**
     * 查询数据源的表列表
     */
    @PreAuthorize("@ss.hasPermi('datasource:query')")
    @GetMapping("/{id}/tables")
    public AjaxResult getTableList(@PathVariable("id") Long id)
    {
        try
        {
            List<java.util.Map<String, Object>> tableList = dataSourceService.getTableList(id);
            return success(tableList);
        }
        catch (Exception e)
        {
            return error("查询表列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询指定表的字段列表
     */
    @PreAuthorize("@ss.hasPermi('datasource:query')")
    @GetMapping("/{id}/tables/{tableName}/columns")
    public AjaxResult getColumnList(@PathVariable("id") Long id, @PathVariable("tableName") String tableName)
    {
        try
        {
            List<java.util.Map<String, Object>> columnList = dataSourceService.getColumnList(id, tableName);
            return success(columnList);
        }
        catch (Exception e)
        {
            return error("查询字段列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询本地数据库的表列表（用于ETL目标表选择、元数据管理）
     */
    @PreAuthorize("@ss.hasPermi('datasource:query')")
    @GetMapping("/local/tables")
    public AjaxResult getLocalTableList()
    {
        try
        {
            List<java.util.Map<String, Object>> tableList = dataSourceService.getLocalTableList();
            return success(tableList);
        }
        catch (Exception e)
        {
            return error("查询本地表列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询本地数据库指定表的字段列表（用于元数据字段管理）
     */
    @PreAuthorize("@ss.hasPermi('datasource:query')")
    @GetMapping("/local/tables/{tableName}/columns")
    public AjaxResult getLocalColumnList(@PathVariable("tableName") String tableName)
    {
        try
        {
            List<java.util.Map<String, Object>> columnList = dataSourceService.getLocalColumnList(tableName);
            return success(columnList);
        }
        catch (Exception e)
        {
            return error("查询本地表字段列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据源表结构自动创建目标表
     */
    @PreAuthorize("@ss.hasPermi('datasource:add')")
    @PostMapping("/create-target-table")
    public AjaxResult createTargetTable(@RequestBody java.util.Map<String, Object> params)
    {
        try
        {
            Long sourceDataSourceId = Long.valueOf(params.get("sourceDataSourceId").toString());
            String sourceTableName = params.get("sourceTableName").toString();
            String targetTableName = params.get("targetTableName").toString();
            
            boolean result = dataSourceService.createTargetTableFromSource(sourceDataSourceId, sourceTableName, targetTableName);
            if (result)
            {
                return success("目标表创建成功");
            }
            else
            {
                return error("目标表创建失败");
            }
        }
        catch (Exception e)
        {
            return error("创建目标表失败: " + e.getMessage());
        }
    }
}
