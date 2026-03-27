package dev.lhl.query.domain.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 歧义按表汇总项（用于歧义优化页汇总视图）
 *
 * @author smart-bi
 */
public class TableErrorSummary
{
    private String tableName;
    private int totalCount;
    private List<CategoryCount> categories = new ArrayList<>();

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public List<CategoryCount> getCategories() { return categories; }
    public void setCategories(List<CategoryCount> categories) { this.categories = categories != null ? categories : new ArrayList<>(); }

    public static class CategoryCount
    {
        private String errorCategory;
        private int count;

        public String getErrorCategory() { return errorCategory; }
        public void setErrorCategory(String errorCategory) { this.errorCategory = errorCategory; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }
}
