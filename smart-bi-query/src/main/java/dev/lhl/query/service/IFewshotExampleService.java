package dev.lhl.query.service;

import dev.lhl.query.domain.FewshotExample;

import java.util.List;

/**
 * Few-shot 示例服务接口
 */
public interface IFewshotExampleService
{
    FewshotExample selectById(Long id);

    List<FewshotExample> selectList(FewshotExample query);

    int create(FewshotExample example);

    int update(FewshotExample example);

    int delete(Long id);

    int updateEnabled(Long id, Integer enabled);

    int batchUpdateEnabled(List<Long> ids, Integer enabled);

    /**
     * 从反馈记录导入为 few-shot 示例
     */
    int importFromFeedback(Long feedbackId, Long createdBy);
}
