package dev.lhl.query.service;

import dev.lhl.query.domain.QueryRecord;
import java.util.List;
import java.util.Map;

/**
 * 查询执行服务接口
 * 负责执行SQL查询，包括COUNT探针检查、结果脱敏等
 * 
 * @author smart-bi
 */
public interface IQueryExecutionService
{
    /**
     * 执行查询
     * 
     * @param queryRecord 查询记录（包含SQL）
     * @param userId 用户ID
     * @return 查询结果
     */
    QueryResult executeQuery(QueryRecord queryRecord, Long userId);
    
    /**
     * 执行COUNT探针检查
     * 在执行完整查询前，先执行COUNT查询检查是否有数据
     * 
     * @param sql SQL语句
     * @param userId 用户ID
     * @return 数据行数，如果为0则不需要执行完整查询
     */
    long countProbe(String sql, Long userId);
    
    /**
     * 查询结果封装
     */
    class QueryResult
    {
        private boolean success;
        private List<Map<String, Object>> data;
        private long rowCount;
        private String errorMessage;
        private long executionTime; // 执行耗时（毫秒）
        /** 是否为数据库执行错误（可触发自修正）；仅当 success=false 时有效 */
        private boolean retriable;

        public QueryResult(boolean success, List<Map<String, Object>> data, long rowCount, String errorMessage, long executionTime)
        {
            this(success, data, rowCount, errorMessage, executionTime, false);
        }

        public QueryResult(boolean success, List<Map<String, Object>> data, long rowCount, String errorMessage, long executionTime, boolean retriable)
        {
            this.success = success;
            this.data = data;
            this.rowCount = rowCount;
            this.errorMessage = errorMessage;
            this.executionTime = executionTime;
            this.retriable = retriable;
        }

        public static QueryResult success(List<Map<String, Object>> data, long rowCount, long executionTime)
        {
            return new QueryResult(true, data, rowCount, null, executionTime);
        }

        public static QueryResult failure(String errorMessage, long executionTime)
        {
            return new QueryResult(false, null, 0, errorMessage, executionTime, false);
        }

        /** 执行失败且为数据库执行错误时可重试（自修正） */
        public static QueryResult failure(String errorMessage, long executionTime, boolean retriable)
        {
            return new QueryResult(false, null, 0, errorMessage, executionTime, retriable);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public List<Map<String, Object>> getData() { return data; }
        public long getRowCount() { return rowCount; }
        public String getErrorMessage() { return errorMessage; }
        public long getExecutionTime() { return executionTime; }
        public boolean isRetriable() { return retriable; }
    }
}
