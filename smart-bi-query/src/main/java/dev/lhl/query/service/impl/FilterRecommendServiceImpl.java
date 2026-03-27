package dev.lhl.query.service.impl;

import dev.lhl.metadata.service.IMetadataService;
import dev.lhl.metadata.domain.FieldMetadata;
import dev.lhl.query.service.IFilterRecommendService;
import dev.lhl.query.service.LlmService;
import dev.lhl.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.alibaba.fastjson2.JSON;

/**
 * 筛选器推荐服务实现
 * 使用LLM分析问题意图，生成筛选器配置
 * 
 * @author smart-bi
 */
@Service
public class FilterRecommendServiceImpl implements IFilterRecommendService
{
    private static final Logger log = LoggerFactory.getLogger(FilterRecommendServiceImpl.class);
    
    @Autowired
    private LlmService llmService;
    
    @Autowired(required = false)
    private IMetadataService metadataService;
    
    @Override
    public List<FilterConfig> recommendFilters(String question, List<String> tableNames, Long userId)
    {
        if (StringUtils.isEmpty(question) || tableNames == null || tableNames.isEmpty())
        {
            log.debug("问题或表名为空，返回空筛选器列表");
            return Collections.emptyList();
        }
        
        try
        {
            log.info("开始推荐筛选器: question={}, tableNames={}, userId={}", question, tableNames, userId);
            
            // 1. 获取表的字段元数据
            Map<String, FieldMetadata> fieldMetadataMap = getFieldMetadataMap(tableNames);
            
            // 2. 使用LLM分析问题意图，识别需要的筛选条件
            String filterAnalysis = analyzeFilterIntent(question, fieldMetadataMap);
            
            // 3. 解析LLM返回的筛选器配置
            List<FilterConfig> filters = parseFilterConfig(filterAnalysis, fieldMetadataMap);
            
            // 4. 为筛选器补充元数据信息（类型、可选值等）
            enrichFilterConfig(filters, fieldMetadataMap);
            
            log.info("筛选器推荐完成: question={}, filterCount={}", question, filters.size());
            return filters;
        }
        catch (Exception e)
        {
            log.error("筛选器推荐失败: question={}", question, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取表的字段元数据
     */
    private Map<String, FieldMetadata> getFieldMetadataMap(List<String> tableNames)
    {
        Map<String, FieldMetadata> fieldMap = new HashMap<>();
        
        if (metadataService == null)
        {
            log.warn("MetadataService未配置，无法获取字段元数据");
            return fieldMap;
        }
        
        try
        {
            for (String tableName : tableNames)
            {
                // 查询表元数据
                dev.lhl.metadata.domain.TableMetadata tableQuery = new dev.lhl.metadata.domain.TableMetadata();
                tableQuery.setTableName(tableName);
                List<dev.lhl.metadata.domain.TableMetadata> tables = metadataService.selectTableMetadataList(tableQuery);
                
                if (tables != null && !tables.isEmpty())
                {
                    Long tableId = tables.get(0).getId();
                    List<FieldMetadata> fields = metadataService.selectFieldMetadataListByTableId(tableId);
                    
                    for (FieldMetadata field : fields)
                    {
                        String key = tableName + "." + field.getFieldName();
                        fieldMap.put(key, field);
                        // 也支持仅字段名作为key
                        fieldMap.put(field.getFieldName(), field);
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.warn("获取字段元数据失败: tableNames={}", tableNames, e);
        }
        
        return fieldMap;
    }
    
    /**
     * 使用LLM分析问题意图，识别筛选条件
     */
    private String analyzeFilterIntent(String question, Map<String, FieldMetadata> fieldMetadataMap)
    {
        try
        {
            // 构建字段信息字符串
            StringBuilder fieldInfo = new StringBuilder();
            for (Map.Entry<String, FieldMetadata> entry : fieldMetadataMap.entrySet())
            {
                FieldMetadata field = entry.getValue();
                fieldInfo.append("字段名: ").append(field.getFieldName())
                    .append(", 类型: ").append(field.getFieldType())
                    .append(", 注释: ").append(field.getFieldComment())
                    .append("\n");
            }
            
            // 构建提示词
            String promptText = String.format(
                "分析以下自然语言问题，识别需要哪些筛选条件。\n\n" +
                "问题: %s\n\n" +
                "可用字段:\n%s\n\n" +
                "请返回JSON格式的筛选器配置列表，格式如下：\n" +
                "[\n" +
                "  {\"fieldName\": \"字段名\", \"filterType\": \"text|number|date|select\", \"required\": true/false}\n" +
                "]\n\n" +
                "筛选器类型说明：\n" +
                "- text: 文本输入框\n" +
                "- number: 数字输入框\n" +
                "- date: 日期选择器\n" +
                "- select: 单选下拉框\n" +
                "- multiSelect: 多选下拉框\n\n" +
                "只返回JSON，不要其他说明文字。",
                question,
                fieldInfo.toString()
            );
            
            Prompt prompt = new Prompt(promptText);
            String response = llmService.generateSQL(prompt);
            
            log.debug("LLM筛选器分析结果: {}", response);
            return response;
        }
        catch (Exception e)
        {
            log.error("LLM分析筛选器意图失败: question={}", question, e);
            return "[]";
        }
    }
    
    /**
     * 解析LLM返回的筛选器配置
     */
    private List<FilterConfig> parseFilterConfig(String filterAnalysis, Map<String, FieldMetadata> fieldMetadataMap)
    {
        List<FilterConfig> filters = new ArrayList<>();
        
        if (StringUtils.isEmpty(filterAnalysis))
        {
            return filters;
        }
        
        try
        {
            // 提取JSON部分（移除可能的markdown代码块标记）
            String jsonStr = filterAnalysis.trim();
            if (jsonStr.startsWith("```json"))
            {
                jsonStr = jsonStr.substring(7);
            }
            if (jsonStr.startsWith("```"))
            {
                jsonStr = jsonStr.substring(3);
            }
            if (jsonStr.endsWith("```"))
            {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            jsonStr = jsonStr.trim();
            
            // 解析JSON
            List<Map<String, Object>> filterList = com.alibaba.fastjson2.JSON.parseObject(
                jsonStr,
                new com.alibaba.fastjson2.TypeReference<List<Map<String, Object>>>() {}
            );
            
            for (Map<String, Object> filterMap : filterList)
            {
                String fieldName = (String) filterMap.get("fieldName");
                String filterType = (String) filterMap.get("filterType");
                Boolean required = (Boolean) filterMap.get("required");
                
                if (StringUtils.isEmpty(fieldName) || StringUtils.isEmpty(filterType))
                {
                    continue;
                }
                
                FilterConfig config = new FilterConfig();
                config.setFieldName(fieldName);
                config.setFilterType(filterType);
                config.setRequired(required != null ? required : false);
                
                // 设置字段显示名
                FieldMetadata field = fieldMetadataMap.get(fieldName);
                if (field != null && StringUtils.isNotEmpty(field.getFieldComment()))
                {
                    config.setFieldLabel(field.getFieldComment());
                }
                else
                {
                    config.setFieldLabel(fieldName);
                }
                
                filters.add(config);
            }
        }
        catch (Exception e)
        {
            log.warn("解析筛选器配置失败: filterAnalysis={}", filterAnalysis, e);
            // 如果JSON解析失败，尝试使用正则表达式提取
            filters.addAll(parseFilterConfigFallback(filterAnalysis, fieldMetadataMap));
        }
        
        return filters;
    }
    
    /**
     * 备用解析方法（正则表达式）
     */
    private List<FilterConfig> parseFilterConfigFallback(String filterAnalysis, Map<String, FieldMetadata> fieldMetadataMap)
    {
        List<FilterConfig> filters = new ArrayList<>();
        
        // 简单的正则匹配：查找 "fieldName": "xxx" 模式
        Pattern pattern = Pattern.compile("\"fieldName\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(filterAnalysis);
        
        while (matcher.find())
        {
            String fieldName = matcher.group(1);
            FieldMetadata field = fieldMetadataMap.get(fieldName);
            
            if (field != null)
            {
                FilterConfig config = new FilterConfig();
                config.setFieldName(fieldName);
                config.setFieldLabel(StringUtils.isNotEmpty(field.getFieldComment()) ? 
                    field.getFieldComment() : fieldName);
                
                // 根据字段类型推断筛选器类型
                String fieldType = field.getFieldType();
                if (fieldType != null)
                {
                    String upperType = fieldType.toUpperCase();
                    if (upperType.contains("DATE") || upperType.contains("TIME"))
                    {
                        config.setFilterType("date");
                    }
                    else if (upperType.contains("INT") || upperType.contains("DECIMAL") || upperType.contains("NUMERIC"))
                    {
                        config.setFilterType("number");
                    }
                    else
                    {
                        config.setFilterType("text");
                    }
                }
                else
                {
                    config.setFilterType("text");
                }
                
                filters.add(config);
            }
        }
        
        return filters;
    }
    
    /**
     * 为筛选器补充元数据信息
     */
    private void enrichFilterConfig(List<FilterConfig> filters, Map<String, FieldMetadata> fieldMetadataMap)
    {
        for (FilterConfig filter : filters)
        {
            FieldMetadata field = fieldMetadataMap.get(filter.getFieldName());
            if (field == null)
            {
                continue;
            }
            
            // 如果是select类型，尝试获取可选值
            if ("select".equals(filter.getFilterType()) || "multiSelect".equals(filter.getFilterType()))
            {
                // 1. 优先使用字段元数据中配置的枚举值
                if (StringUtils.isNotEmpty(field.getEnumValues()))
                {
                    try
                    {
                        // 解析JSON格式的枚举值
                        List<Object> options = parseEnumValues(field.getEnumValues());
                        if (options != null && !options.isEmpty())
                        {
                            filter.setOptions(options);
                            log.debug("从字段元数据获取枚举值: fieldName={}, optionCount={}", filter.getFieldName(), options.size());
                        }
                    }
                    catch (Exception e)
                    {
                        log.warn("解析字段枚举值失败: fieldName={}, enumValues={}", filter.getFieldName(), field.getEnumValues(), e);
                    }
                }
                
                // 2. 如果字段元数据中没有枚举值，且需要从数据库查询
                // 注意：这需要表元数据关联数据源信息，当前架构暂不支持
                // 如果需要此功能，建议：
                // - 在TableMetadata中添加datasourceId字段
                // - 或者通过业务域（Domain）关联数据源
                // 当前实现：如果enumValues为空，options也为空，前端可以手动输入
                if (filter.getOptions() == null || filter.getOptions().isEmpty())
                {
                    log.debug("字段元数据未配置枚举值，无法从数据库查询: fieldName={}", filter.getFieldName());
                }
            }
            
            // 设置提示信息
            if (StringUtils.isEmpty(filter.getPlaceholder()))
            {
                filter.setPlaceholder("请输入" + filter.getFieldLabel());
            }
        }
    }
    
    /**
     * 解析枚举值（JSON格式）
     * 支持格式：
     * 1. ["value1", "value2", "value3"] - 字符串数组
     * 2. [{"label": "标签1", "value": "value1"}, ...] - 对象数组
     * 3. {"value1": "标签1", "value2": "标签2"} - 键值对对象
     */
    private List<Object> parseEnumValues(String enumValuesJson)
    {
        if (StringUtils.isEmpty(enumValuesJson))
        {
            return Collections.emptyList();
        }
        
        try
        {
            // 尝试解析为数组
            Object parsed = JSON.parse(enumValuesJson);
            
            if (parsed instanceof List)
            {
                List<?> list = (List<?>) parsed;
                List<Object> options = new ArrayList<>();
                for (Object item : list)
                {
                    if (item instanceof Map)
                    {
                        // 对象格式：{"label": "标签", "value": "值"}
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) item;
                        if (map.containsKey("value"))
                        {
                            options.add(map.get("value"));
                        }
                        else if (map.containsKey("label"))
                        {
                            options.add(map.get("label"));
                        }
                        else
                        {
                            // 取第一个值
                            options.add(map.values().iterator().next());
                        }
                    }
                    else
                    {
                        // 直接值
                        options.add(item);
                    }
                }
                return options;
            }
            else if (parsed instanceof Map)
            {
                // 键值对格式：{"value1": "标签1", "value2": "标签2"}
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) parsed;
                return new ArrayList<>(map.keySet());
            }
            else
            {
                // 单个值
                return Collections.singletonList(parsed);
            }
        }
        catch (Exception e)
        {
            log.warn("解析枚举值失败: enumValuesJson={}", enumValuesJson, e);
            return Collections.emptyList();
        }
    }
}
