package dev.lhl.dashboard.service;

/**
 * 看板刷新服务接口
 * 负责刷新看板中的卡片数据
 * 
 * @author smart-bi
 */
public interface IDashboardRefreshService
{
    /**
     * 刷新看板
     * 重新执行看板中所有卡片关联的SQL查询，更新卡片数据
     * 
     * @param dashboardId 看板ID
     * @return 刷新结果（成功刷新的卡片数量、失败的卡片数量）
     */
    RefreshResult refreshDashboard(Long dashboardId);
    
    /**
     * 刷新单个卡片
     * 
     * @param cardId 卡片ID
     * @return 是否刷新成功
     */
    boolean refreshCard(Long cardId);
    
    /**
     * 执行图表卡 SQL 并返回图表数据（不落库），用于前端展示与定时刷新
     *
     * @param cardId 图表卡 ID
     * @param userId 当前用户 ID（用于权限与执行上下文）
     * @return 成功时返回 { type, columns, data }，无 SQL 或执行失败时返回 null
     */
    java.util.Map<String, Object> getChartCardData(Long cardId, Long userId);
    
    /**
     * 刷新结果
     */
    class RefreshResult
    {
        private int successCount;
        private int failCount;
        private String message;
        
        public RefreshResult(int successCount, int failCount, String message)
        {
            this.successCount = successCount;
            this.failCount = failCount;
            this.message = message;
        }
        
        public static RefreshResult success(int successCount)
        {
            return new RefreshResult(successCount, 0, "刷新成功");
        }
        
        public static RefreshResult failure(int failCount, String message)
        {
            return new RefreshResult(0, failCount, message);
        }
        
        public static RefreshResult partial(int successCount, int failCount, String message)
        {
            return new RefreshResult(successCount, failCount, message);
        }
        
        // Getters
        public int getSuccessCount() { return successCount; }
        public int getFailCount() { return failCount; }
        public String getMessage() { return message; }
    }
}
