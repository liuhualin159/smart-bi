package dev.lhl.query.service;

import dev.lhl.query.domain.QuerySession;
import java.util.List;
import java.util.Map;

/**
 * 对话上下文管理服务接口
 * 负责管理多轮对话的上下文信息
 * 
 * @author smart-bi
 */
public interface IConversationService
{
    /**
     * 创建或获取会话
     * 
     * @param userId 用户ID
     * @param sessionKey 会话键（前端生成，可选）
     * @return 会话对象
     */
    QuerySession createOrGetSession(Long userId, String sessionKey);
    
    /**
     * 获取会话
     * 
     * @param sessionId 会话ID
     * @return 会话对象
     */
    QuerySession getSession(Long sessionId);
    
    /**
     * 获取会话（通过sessionKey）
     * 
     * @param userId 用户ID
     * @param sessionKey 会话键
     * @return 会话对象
     */
    QuerySession getSessionByKey(Long userId, String sessionKey);
    
    /**
     * 更新会话上下文
     * 
     * @param sessionId 会话ID
     * @param question 当前问题
     * @param answer 当前回答（SQL或结果摘要）
     * @return 是否成功
     */
    boolean updateContext(Long sessionId, String question, String answer);
    
    /**
     * 获取会话上下文（用于LLM）
     * 
     * @param sessionId 会话ID
     * @return 上下文列表（包含历史问答对）
     */
    List<Map<String, String>> getContextForLLM(Long sessionId);
    
    /**
     * 清除过期会话
     * 删除30分钟无操作的会话
     * 
     * @return 清除的会话数量
     */
    int clearExpiredSessions();
    
    /**
     * 删除会话
     * 
     * @param sessionId 会话ID
     * @return 是否成功
     */
    boolean deleteSession(Long sessionId);
}
