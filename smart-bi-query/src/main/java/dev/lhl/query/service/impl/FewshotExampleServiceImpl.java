package dev.lhl.query.service.impl;

import dev.lhl.query.domain.FewshotExample;
import dev.lhl.query.domain.Feedback;
import dev.lhl.query.domain.QueryRecord;
import dev.lhl.query.mapper.FewshotExampleMapper;
import dev.lhl.query.service.IFewshotExampleService;
import dev.lhl.query.service.IFeedbackService;
import dev.lhl.query.service.IQueryRecordService;
import dev.lhl.metadata.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Few-shot 示例服务实现
 * 创建/更新/删除时同步向量到 VectorStore（与表 id 关联，用于相似检索）
 */
@Service
public class FewshotExampleServiceImpl implements IFewshotExampleService
{
    private static final Logger log = LoggerFactory.getLogger(FewshotExampleServiceImpl.class);

    private static final String FEWSHOT_VECTOR_ID_PREFIX = "fewshot_";

    @Autowired
    private FewshotExampleMapper fewshotExampleMapper;

    @Autowired(required = false)
    private IFeedbackService feedbackService;

    @Autowired(required = false)
    private IQueryRecordService queryRecordService;

    @Autowired(required = false)
    private VectorStoreService vectorStoreService;

    @Override
    public FewshotExample selectById(Long id)
    {
        return fewshotExampleMapper.selectById(id);
    }

    @Override
    public List<FewshotExample> selectList(FewshotExample query)
    {
        return fewshotExampleMapper.selectList(query);
    }

    @Override
    public int create(FewshotExample example)
    {
        if (example.getEnabled() == null)
        {
            example.setEnabled(1);
        }
        int rows = fewshotExampleMapper.insert(example);
        if (rows > 0 && example.getId() != null && vectorStoreService != null)
        {
            try
            {
                storeFewshotVector(example);
            }
            catch (Exception e)
            {
                log.warn("Few-shot 示例向量写入失败，示例已落库: id={}", example.getId(), e);
            }
        }
        return rows;
    }

    @Override
    public int update(FewshotExample example)
    {
        if (example == null || example.getId() == null)
        {
            return 0;
        }
        FewshotExample old = fewshotExampleMapper.selectById(example.getId());
        int rows = fewshotExampleMapper.update(example);
        if (rows > 0 && vectorStoreService != null && old != null)
        {
            boolean questionOrSqlChanged = !java.util.Objects.equals(example.getQuestion(), old.getQuestion())
                || !java.util.Objects.equals(example.getSqlText(), old.getSqlText());
            if (questionOrSqlChanged)
            {
                try
                {
                    FewshotExample updated = fewshotExampleMapper.selectById(example.getId());
                    if (updated != null)
                    {
                        updateFewshotVector(updated);
                    }
                }
                catch (Exception e)
                {
                    log.warn("Few-shot 示例向量更新失败: id={}", example.getId(), e);
                }
            }
        }
        return rows;
    }

    @Override
    public int delete(Long id)
    {
        if (id == null) return 0;
        if (vectorStoreService != null)
        {
            try
            {
                vectorStoreService.delete(FEWSHOT_VECTOR_ID_PREFIX + id);
            }
            catch (Exception e)
            {
                log.warn("Few-shot 示例向量删除失败（继续删除 DB 记录）: id={}", id, e);
            }
        }
        return fewshotExampleMapper.deleteById(id);
    }

    @Override
    public int updateEnabled(Long id, Integer enabled)
    {
        return fewshotExampleMapper.updateEnabled(id, enabled);
    }

    @Override
    public int batchUpdateEnabled(List<Long> ids, Integer enabled)
    {
        if (ids == null || ids.isEmpty()) return 0;
        return fewshotExampleMapper.batchUpdateEnabled(ids, enabled);
    }

    @Override
    public int importFromFeedback(Long feedbackId, Long createdBy)
    {
        if (feedbackService == null || queryRecordService == null)
        {
            throw new RuntimeException("反馈服务或查询记录服务未配置");
        }

        Feedback feedback = feedbackService.selectFeedbackById(feedbackId);
        if (feedback == null)
        {
            throw new RuntimeException("反馈记录不存在: " + feedbackId);
        }

        QueryRecord record = queryRecordService.selectQueryRecordById(feedback.getQueryId());
        if (record == null)
        {
            throw new RuntimeException("关联查询记录不存在");
        }

        String question = record.getQuestion();
        String sql = dev.lhl.common.utils.StringUtils.isNotEmpty(feedback.getSuggestedSql())
            ? feedback.getSuggestedSql()
            : record.getGeneratedSql();

        if (dev.lhl.common.utils.StringUtils.isEmpty(question) || dev.lhl.common.utils.StringUtils.isEmpty(sql))
        {
            throw new RuntimeException("问题或SQL为空，无法导入");
        }

        FewshotExample example = new FewshotExample();
        example.setQuestion(question);
        example.setSqlText(sql);
        example.setEnabled(1);
        example.setCreatedBy(createdBy);

        int rows = fewshotExampleMapper.insert(example);
        if (rows > 0 && example.getId() != null && vectorStoreService != null)
        {
            try
            {
                storeFewshotVector(example);
            }
            catch (Exception e)
            {
                log.warn("从反馈导入 Few-shot 示例向量写入失败: id={}", example.getId(), e);
            }
        }
        return rows;
    }

    /**
     * 将示例写入向量存储（用于相似检索）
     * 文本使用 question，便于按用户问题召回
     */
    private void storeFewshotVector(FewshotExample example)
    {
        String vectorId = FEWSHOT_VECTOR_ID_PREFIX + example.getId();
        String text = buildTextForEmbedding(example);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "fewshot_example");
        metadata.put("fewshotId", example.getId());
        metadata.put("datasourceId", example.getDatasourceId());
        metadata.put("enabled", example.getEnabled() != null ? example.getEnabled() : 1);
        vectorStoreService.store(vectorId, text, metadata);
        log.debug("Few-shot 示例向量已写入: id={}", example.getId());
    }

    /**
     * 更新示例对应向量（question/sql 变更时重新计算并写入）
     */
    private void updateFewshotVector(FewshotExample example)
    {
        String vectorId = FEWSHOT_VECTOR_ID_PREFIX + example.getId();
        String text = buildTextForEmbedding(example);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "fewshot_example");
        metadata.put("fewshotId", example.getId());
        metadata.put("datasourceId", example.getDatasourceId());
        metadata.put("enabled", example.getEnabled() != null ? example.getEnabled() : 1);
        vectorStoreService.update(vectorId, text, metadata);
        log.debug("Few-shot 示例向量已更新: id={}", example.getId());
    }

    /** 用于向量化的文本：优先 question，便于按问题相似度召回 */
    private static String buildTextForEmbedding(FewshotExample example)
    {
        String q = example.getQuestion() != null ? example.getQuestion().trim() : "";
        String sql = example.getSqlText() != null ? example.getSqlText().trim() : "";
        if (q.isEmpty()) return sql;
        if (sql.isEmpty()) return q;
        return q + "\n" + sql;
    }
}
