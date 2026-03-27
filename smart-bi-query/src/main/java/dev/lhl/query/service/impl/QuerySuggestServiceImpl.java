package dev.lhl.query.service.impl;

import dev.lhl.query.service.IQuerySuggestService;
import dev.lhl.query.service.IQueryRecordService;
import dev.lhl.query.domain.QueryRecord;
import dev.lhl.metadata.service.IMetadataService;
import dev.lhl.metadata.service.VectorSearchService;
import dev.lhl.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 查询建议服务实现
 * 基于向量检索或关键字匹配提供查询建议
 * 
 * @author smart-bi
 */
@Service
public class QuerySuggestServiceImpl implements IQuerySuggestService
{
    private static final Logger log = LoggerFactory.getLogger(QuerySuggestServiceImpl.class);
    
    @Autowired(required = false)
    private VectorSearchService vectorSearchService;
    
    @Autowired(required = false)
    private IMetadataService metadataService;
    
    @Autowired(required = false)
    private IQueryRecordService queryRecordService;
    
    @Override
    public List<QuerySuggestion> getSuggestions(String text, Integer limit, Long userId)
    {
        if (StringUtils.isEmpty(text))
        {
            return Collections.emptyList();
        }
        
        if (limit == null || limit <= 0)
        {
            limit = 10;
        }
        
        try
        {
            log.debug("获取查询建议: text={}, limit={}, userId={}", text, limit, userId);
            
            List<QuerySuggestion> suggestions = new ArrayList<>();
            
            // 1. 尝试使用向量检索（如果可用）
            if (vectorSearchService != null)
            {
                try
                {
                    List<Map<String, Object>> vectorResults = vectorSearchService.search(text, limit);
                    for (Map<String, Object> result : vectorResults)
                    {
                        String content = (String) result.get("content");
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> metadata = (java.util.Map<String, Object>) result.get("metadata");
                        Double similarity = ((Number) result.get("similarity")).doubleValue();
                        
                        if (content != null)
                        {
                            String type = metadata != null ? (String) metadata.get("type") : "UNKNOWN";
                            String description = metadata != null ? (String) metadata.get("description") : content;
                            
                            suggestions.add(new QuerySuggestion(content, type, description, similarity));
                        }
                    }
                }
                catch (Exception e)
                {
                    log.warn("向量检索失败，降级为关键字匹配: text={}", text, e);
                }
            }
            
            // 2. 如果向量检索不可用或结果不足，使用降级方案
            if (suggestions.size() < limit)
            {
                // 2.1 从查询历史中获取建议
                if (queryRecordService != null && userId != null)
                {
                    try
                    {
                        QueryRecord queryRecord = new QueryRecord();
                        queryRecord.setUserId(userId);
                        List<QueryRecord> history = queryRecordService.selectQueryRecordList(queryRecord);
                        
                        // 从历史记录中匹配问题
                        for (QueryRecord record : history)
                        {
                            if (record.getQuestion() != null && 
                                record.getQuestion().toLowerCase().contains(text.toLowerCase()))
                            {
                                suggestions.add(new QuerySuggestion(
                                    record.getQuestion(),
                                    "QUESTION",
                                    "历史查询",
                                    0.5 // 关键字匹配的分数较低
                                ));
                                
                                if (suggestions.size() >= limit)
                                {
                                    break;
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        log.warn("从查询历史获取建议失败", e);
                    }
                }
                
                // 2.2 从元数据中获取建议（关键字匹配）
                if (metadataService != null && suggestions.size() < limit)
                {
                    try
                    {
                        // 查询表元数据
                        dev.lhl.metadata.domain.TableMetadata tableQuery = new dev.lhl.metadata.domain.TableMetadata();
                        List<dev.lhl.metadata.domain.TableMetadata> tables = metadataService.selectTableMetadataList(tableQuery);
                        
                        for (dev.lhl.metadata.domain.TableMetadata table : tables)
                        {
                            if (table.getTableName() != null && 
                                table.getTableName().toLowerCase().contains(text.toLowerCase()))
                            {
                                suggestions.add(new QuerySuggestion(
                                    table.getTableName(),
                                    "TABLE",
                                    table.getTableComment() != null ? table.getTableComment() : "表",
                                    0.4
                                ));
                                
                                if (suggestions.size() >= limit)
                                {
                                    break;
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        log.warn("从元数据获取建议失败", e);
                    }
                }
            }
            
            // 3. 去重并按分数排序
            suggestions = suggestions.stream()
                .collect(Collectors.toMap(
                    QuerySuggestion::getText,
                    s -> s,
                    (s1, s2) -> s1.getScore() > s2.getScore() ? s1 : s2
                ))
                .values()
                .stream()
                .sorted((s1, s2) -> Double.compare(s2.getScore(), s1.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
            
            log.debug("查询建议完成: text={}, suggestionsCount={}", text, suggestions.size());
            return suggestions;
        }
        catch (Exception e)
        {
            log.error("获取查询建议失败: text={}", text, e);
            return Collections.emptyList();
        }
    }
}
