package dev.lhl.query.service;

import dev.lhl.metadata.service.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Few-shot 示例检索服务
 * 基于向量检索从 Qdrant fewshot_examples 集合中查找相似示例
 */
@Service
public class FewshotRetrievalService
{
    private static final Logger log = LoggerFactory.getLogger(FewshotRetrievalService.class);

    @Value("${smart.bi.nl2sql.fewshot.enabled:true}")
    private boolean enabled;

    @Value("${smart.bi.nl2sql.fewshot.topK:3}")
    private int topK;

    @Value("${smart.bi.nl2sql.fewshot.similarityThreshold:0.7}")
    private double similarityThreshold;

    @Autowired(required = false)
    private VectorSearchService vectorSearchService;

    @Autowired(required = false)
    private dev.lhl.query.mapper.FewshotExampleMapper fewshotExampleMapper;

    /**
     * 检索与当前问题最相似的 few-shot 示例
     * 优先使用向量检索（VectorStore 中 type=fewshot_example 的向量），不可用时降级为 DB+关键词匹配
     *
     * @param question 用户问题
     * @param datasourceId 当前数据源ID（可为null）
     * @return 示例列表，每项含 question 和 sql
     */
    public List<Map<String, String>> retrieveExamples(String question, Long datasourceId)
    {
        if (!enabled || fewshotExampleMapper == null)
        {
            return Collections.emptyList();
        }

        // 优先向量检索（与 4.3 写入的 fewshot_example 向量一致）
        if (vectorSearchService != null)
        {
            try
            {
                List<Map<String, String>> fromVector = retrieveExamplesByVector(question, datasourceId);
                if (!fromVector.isEmpty())
                {
                    return fromVector;
                }
            }
            catch (Exception e)
            {
                log.warn("Few-shot 向量检索失败，降级为 DB 检索: {}", e.getMessage());
            }
        }

        return retrieveExamplesByDb(question, datasourceId);
    }

    /**
     * 从向量检索结果中过滤 fewshot_example 类型，并按相似度阈值与 datasourceId 筛选后取 topK
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, String>> retrieveExamplesByVector(String question, Long datasourceId)
    {
        int fetchLimit = Math.max(topK * 5, 20);
        List<Map<String, Object>> raw = vectorSearchService.search(question, fetchLimit);
        if (raw == null || raw.isEmpty()) return Collections.emptyList();

        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, Object> item : raw)
        {
            Object metaObj = item.get("metadata");
            if (!(metaObj instanceof Map)) continue;
            Map<String, Object> meta = (Map<String, Object>) metaObj;
            if (!"fewshot_example".equals(meta.get("type"))) continue;
            Object enabledObj = meta.get("enabled");
            if (enabledObj != null && Integer.valueOf(0).equals(enabledObj)) continue;
            Object dsId = meta.get("datasourceId");
            if (dsId != null && datasourceId != null && !dsId.toString().equals(datasourceId.toString())) continue;

            Object simObj = item.get("similarity");
            double sim = simObj instanceof Number ? ((Number) simObj).doubleValue() : 0;
            if (sim < similarityThreshold) continue;

            Object fewshotIdObj = meta.get("fewshotId");
            if (fewshotIdObj == null) continue;
            Long fewshotId = fewshotIdObj instanceof Number ? ((Number) fewshotIdObj).longValue() : Long.parseLong(fewshotIdObj.toString());
            dev.lhl.query.domain.FewshotExample ex = fewshotExampleMapper.selectById(fewshotId);
            if (ex == null) continue;
            result.add(Map.of("question", ex.getQuestion() != null ? ex.getQuestion() : "", "sql", ex.getSqlText() != null ? ex.getSqlText() : ""));
            if (result.size() >= topK) break;
        }
        log.debug("向量检索 few-shot 示例数: {}", result.size());
        return result;
    }

    /** 降级：从 DB 按数据源取 enabled 示例，再做关键词匹配与补全 */
    private List<Map<String, String>> retrieveExamplesByDb(String question, Long datasourceId)
    {
        try
        {
            List<dev.lhl.query.domain.FewshotExample> examples = fewshotExampleMapper.selectEnabledByDatasource(datasourceId, topK * 3);
            if (examples == null || examples.isEmpty())
            {
                return Collections.emptyList();
            }

            List<Map<String, String>> result = new ArrayList<>();
            String questionLower = question.toLowerCase();
            for (dev.lhl.query.domain.FewshotExample ex : examples)
            {
                if (result.size() >= topK) break;
                String exQuestion = ex.getQuestion();
                if (exQuestion != null && hasOverlap(questionLower, exQuestion.toLowerCase()))
                {
                    result.add(Map.of("question", ex.getQuestion(), "sql", ex.getSqlText() != null ? ex.getSqlText() : ""));
                }
            }
            if (result.size() < topK)
            {
                for (dev.lhl.query.domain.FewshotExample ex : examples)
                {
                    if (result.size() >= topK) break;
                    Map<String, String> entry = Map.of("question", ex.getQuestion() != null ? ex.getQuestion() : "", "sql", ex.getSqlText() != null ? ex.getSqlText() : "");
                    if (!result.contains(entry))
                    {
                        result.add(entry);
                    }
                }
            }
            log.debug("DB 检索 few-shot 示例数: {}", result.size());
            return result;
        }
        catch (Exception e)
        {
            log.warn("Few-shot 示例检索失败，降级为不注入示例", e);
            return Collections.emptyList();
        }
    }

    private boolean hasOverlap(String question, String example)
    {
        String[] words = example.split("[\\s，。、？！]+");
        int matchCount = 0;
        for (String w : words)
        {
            if (w.length() >= 2 && question.contains(w))
            {
                matchCount++;
            }
        }
        return matchCount >= 1;
    }
}
