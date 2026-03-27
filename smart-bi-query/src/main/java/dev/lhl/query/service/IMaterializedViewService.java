package dev.lhl.query.service;

import java.util.List;
import java.util.Map;

/**
 * 物化视图服务（占位）
 * MySQL 无原生物化视图，实际实现需通过预聚合表 + 定时刷新模拟
 *
 * @author smart-bi
 */
public interface IMaterializedViewService {

    /**
     * 获取物化视图列表
     */
    List<Map<String, Object>> listMaterializedViews();

    /**
     * 刷新指定物化视图
     *
     * @param viewName 视图名称
     * @return 是否成功
     */
    boolean refresh(String viewName);
}
