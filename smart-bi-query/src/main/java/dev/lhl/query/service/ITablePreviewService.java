package dev.lhl.query.service;

import java.util.List;
import java.util.Map;

/**
 * 表数据预览服务接口（用于数据浏览/探索）
 *
 * @author smart-bi
 */
public interface ITablePreviewService {

    /**
     * 预览指定表的数据
     *
     * @param tableId 表元数据ID
     * @param limit   最大行数，默认100
     * @param userId  当前用户ID（用于权限校验）
     * @return 预览结果：columns 列名列表，data 行数据
     */
    PreviewResult previewTable(Long tableId, int limit, Long userId);

    /**
     * 预览结果
     */
    record PreviewResult(List<String> columns, List<Map<String, Object>> data) {}
}
