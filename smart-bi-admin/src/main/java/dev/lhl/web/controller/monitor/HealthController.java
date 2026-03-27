package dev.lhl.web.controller.monitor;

import dev.lhl.common.core.controller.BaseController;
import dev.lhl.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统健康检查Controller
 * 提供系统健康状态检查接口
 * 
 * @author smart-bi
 */
@RestController
@RequestMapping("/api/monitor/health")
public class HealthController extends BaseController
{
    @Autowired(required = false)
    private dev.lhl.datasource.service.IDataSourceService dataSourceService;
    
    @Autowired(required = false)
    private dev.lhl.metadata.service.VectorSearchService vectorSearchService;
    
    /**
     * 系统健康检查
     */
    @GetMapping
    public AjaxResult health()
    {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        
        // 检查组件状态
        Map<String, Object> components = new HashMap<>();
        
        // 数据库连接
        Map<String, Object> db = new HashMap<>();
        try
        {
            if (dataSourceService != null)
            {
                // 尝试获取本地连接
                java.sql.Connection conn = dataSourceService.getLocalConnection();
                if (conn != null && !conn.isClosed())
                {
                    db.put("status", "UP");
                    conn.close();
                }
                else
                {
                    db.put("status", "DOWN");
                    db.put("error", "数据库连接不可用");
                }
            }
            else
            {
                db.put("status", "UNKNOWN");
                db.put("error", "数据源服务未配置");
            }
        }
        catch (Exception e)
        {
            db.put("status", "DOWN");
            db.put("error", e.getMessage());
        }
        components.put("database", db);
        
        // 向量检索服务
        Map<String, Object> vector = new HashMap<>();
        try
        {
            if (vectorSearchService != null)
            {
                // 简单检查：尝试搜索
                vectorSearchService.search("test", 1);
                vector.put("status", "UP");
            }
            else
            {
                vector.put("status", "UNKNOWN");
                vector.put("error", "向量检索服务未配置");
            }
        }
        catch (Exception e)
        {
            vector.put("status", "DOWN");
            vector.put("error", e.getMessage());
        }
        components.put("vectorSearch", vector);
        
        // 内存使用情况
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        Map<String, Object> memory = new HashMap<>();
        memory.put("total", totalMemory);
        memory.put("used", usedMemory);
        memory.put("free", freeMemory);
        memory.put("max", maxMemory);
        memory.put("usagePercent", (double) usedMemory / maxMemory * 100);
        components.put("memory", memory);
        
        health.put("components", components);
        
        // 判断整体状态
        boolean allUp = true;
        for (Object component : components.values())
        {
            if (component instanceof Map)
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> comp = (Map<String, Object>) component;
                if ("DOWN".equals(comp.get("status")))
                {
                    allUp = false;
                    break;
                }
            }
        }
        
        if (!allUp)
        {
            health.put("status", "DOWN");
        }
        
        return success(health);
    }
}
