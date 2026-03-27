package dev.lhl.query.service;

import java.util.List;

/**
 * 筛选器推荐服务接口
 * 根据自然语言问题推荐合适的筛选条件
 * 
 * @author smart-bi
 */
public interface IFilterRecommendService
{
    /**
     * 推荐筛选器
     * 
     * @param question 自然语言问题
     * @param tableNames 涉及的表名列表
     * @param userId 用户ID
     * @return 筛选器配置列表
     */
    List<FilterConfig> recommendFilters(String question, List<String> tableNames, Long userId);
    
    /**
     * 筛选器配置
     */
    class FilterConfig
    {
        /** 字段名 */
        private String fieldName;
        
        /** 字段显示名 */
        private String fieldLabel;
        
        /** 筛选器类型：text, number, date, select, multiSelect */
        private String filterType;
        
        /** 默认值 */
        private Object defaultValue;
        
        /** 可选值列表（用于select类型） */
        private List<Object> options;
        
        /** 最小值（用于number类型） */
        private Number minValue;
        
        /** 最大值（用于number类型） */
        private Number maxValue;
        
        /** 是否必填 */
        private Boolean required;
        
        /** 提示信息 */
        private String placeholder;
        
        public FilterConfig() {}
        
        public FilterConfig(String fieldName, String fieldLabel, String filterType)
        {
            this.fieldName = fieldName;
            this.fieldLabel = fieldLabel;
            this.filterType = filterType;
        }
        
        // Getters and Setters
        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        
        public String getFieldLabel() { return fieldLabel; }
        public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }
        
        public String getFilterType() { return filterType; }
        public void setFilterType(String filterType) { this.filterType = filterType; }
        
        public Object getDefaultValue() { return defaultValue; }
        public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
        
        public List<Object> getOptions() { return options; }
        public void setOptions(List<Object> options) { this.options = options; }
        
        public Number getMinValue() { return minValue; }
        public void setMinValue(Number minValue) { this.minValue = minValue; }
        
        public Number getMaxValue() { return maxValue; }
        public void setMaxValue(Number maxValue) { this.maxValue = maxValue; }
        
        public Boolean getRequired() { return required; }
        public void setRequired(Boolean required) { this.required = required; }
        
        public String getPlaceholder() { return placeholder; }
        public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }
    }
}
