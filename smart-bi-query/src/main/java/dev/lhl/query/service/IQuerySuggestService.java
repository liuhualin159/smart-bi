package dev.lhl.query.service;

import java.util.List;

/**
 * 查询建议服务接口
 * 基于向量检索或关键字匹配提供查询建议
 * 
 * @author smart-bi
 */
public interface IQuerySuggestService
{
    /**
     * 获取查询建议
     * 
     * @param text 用户输入的文本
     * @param limit 返回建议数量（默认10）
     * @param userId 用户ID（可选，用于个性化建议）
     * @return 建议列表
     */
    List<QuerySuggestion> getSuggestions(String text, Integer limit, Long userId);
    
    /**
     * 查询建议
     */
    class QuerySuggestion
    {
        private String text;
        private String type; // FIELD, METRIC, TABLE, QUESTION
        private String description;
        private Double score; // 相关性分数
        
        public QuerySuggestion() {}
        
        public QuerySuggestion(String text, String type, String description, Double score)
        {
            this.text = text;
            this.type = type;
            this.description = description;
            this.score = score;
        }
        
        // Getters and Setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
    }
}
