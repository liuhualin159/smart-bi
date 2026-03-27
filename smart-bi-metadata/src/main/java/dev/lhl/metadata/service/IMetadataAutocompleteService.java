package dev.lhl.metadata.service;

import dev.lhl.metadata.domain.dto.AutocompleteItem;

import java.util.List;

/**
 * 元数据自动补全服务
 */
public interface IMetadataAutocompleteService
{
    /**
     * 按关键词模糊搜索表、字段、指标，按类型分组，返回最多 maxResults 条
     *
     * @param keyword 关键词（表名、字段名、指标名、注释等）
     * @param userId  当前用户 ID（用于数据权限过滤，可选）
     * @param maxResults 最大返回条数，默认 10
     * @return 补全项列表，每项含 type(table/field/metric)、label、value
     */
    List<AutocompleteItem> search(String keyword, Long userId, int maxResults);
}
