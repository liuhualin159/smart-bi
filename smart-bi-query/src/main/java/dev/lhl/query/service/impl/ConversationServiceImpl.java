package dev.lhl.query.service.impl;

import dev.lhl.query.domain.QuerySession;
import dev.lhl.query.mapper.QuerySessionMapper;
import dev.lhl.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 对话上下文管理服务实现
 * 负责管理多轮对话的上下文信息
 * 
 * @author smart-bi
 */
@Service
public class ConversationServiceImpl implements dev.lhl.query.service.IConversationService
{
    private static final Logger log = LoggerFactory.getLogger(ConversationServiceImpl.class);
    
    // 会话过期时间（30分钟）
    private static final int SESSION_EXPIRE_MINUTES = 30;
    
    @Autowired
    private QuerySessionMapper querySessionMapper;
    
    @Override
    public QuerySession createOrGetSession(Long userId, String sessionKey)
    {
        try
        {
            QuerySession session = null;
            
            // 如果提供了sessionKey，尝试获取现有会话
            if (StringUtils.isNotEmpty(sessionKey))
            {
                session = querySessionMapper.selectQuerySessionByUserIdAndSessionKey(userId, sessionKey);
                
                // 检查会话是否过期
                if (session != null && session.getExpireTime() != null && 
                    session.getExpireTime().before(new java.util.Date()))
                {
                    log.debug("会话已过期，创建新会话: sessionKey={}", sessionKey);
                    session = null;
                }
            }
            
            // 如果会话不存在或已过期，创建新会话
            if (session == null)
            {
                session = new QuerySession();
                session.setUserId(userId);
                
                // 生成新的sessionKey（如果未提供）
                if (StringUtils.isEmpty(sessionKey))
                {
                    sessionKey = generateSessionKey();
                }
                session.setSessionKey(sessionKey);
                
                // 初始化上下文
                session.setContext("[]");
                
                // 设置时间
                java.util.Date now = new java.util.Date();
                session.setLastActiveTime(now);
                session.setCreateTime(now);
                
                // 计算过期时间（30分钟后）
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                calendar.add(java.util.Calendar.MINUTE, SESSION_EXPIRE_MINUTES);
                session.setExpireTime(calendar.getTime());
                
                querySessionMapper.insertQuerySession(session);
                log.debug("创建新会话: sessionId={}, sessionKey={}", session.getId(), sessionKey);
            }
            else
            {
                // 更新最后活跃时间
                updateLastActiveTime(session.getId());
            }
            
            return session;
        }
        catch (Exception e)
        {
            log.error("创建或获取会话失败: userId={}, sessionKey={}", userId, sessionKey, e);
            throw new RuntimeException("创建或获取会话失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public QuerySession getSession(Long sessionId)
    {
        if (sessionId == null)
        {
            return null;
        }
        
        try
        {
            QuerySession session = querySessionMapper.selectQuerySessionById(sessionId);
            
            // 检查是否过期
            if (session != null && session.getExpireTime() != null && 
                session.getExpireTime().before(new java.util.Date()))
            {
                log.debug("会话已过期: sessionId={}", sessionId);
                return null;
            }
            
            return session;
        }
        catch (Exception e)
        {
            log.error("获取会话失败: sessionId={}", sessionId, e);
            return null;
        }
    }
    
    @Override
    public QuerySession getSessionByKey(Long userId, String sessionKey)
    {
        if (StringUtils.isEmpty(sessionKey))
        {
            return null;
        }
        
        try
        {
            QuerySession session = querySessionMapper.selectQuerySessionByUserIdAndSessionKey(userId, sessionKey);
            
            // 检查是否过期
            if (session != null && session.getExpireTime() != null && 
                session.getExpireTime().before(new java.util.Date()))
            {
                log.debug("会话已过期: sessionKey={}", sessionKey);
                return null;
            }
            
            return session;
        }
        catch (Exception e)
        {
            log.error("获取会话失败: userId={}, sessionKey={}", userId, sessionKey, e);
            return null;
        }
    }
    
    @Override
    public boolean updateContext(Long sessionId, String question, String answer)
    {
        try
        {
            QuerySession session = querySessionMapper.selectQuerySessionById(sessionId);
            if (session == null)
            {
                log.warn("会话不存在: sessionId={}", sessionId);
                return false;
            }
            
            // 解析现有上下文
            List<Map<String, String>> contextList = parseContext(session.getContext());
            
            // 添加新的问答对
            Map<String, String> qaPair = new HashMap<>();
            qaPair.put("question", question);
            qaPair.put("answer", answer);
            qaPair.put("timestamp", String.valueOf(System.currentTimeMillis()));
            contextList.add(qaPair);
            
            // 限制上下文长度（最多保留最近10轮对话）
            if (contextList.size() > 10)
            {
                contextList = contextList.subList(contextList.size() - 10, contextList.size());
            }
            
            // 更新上下文
            String newContext = com.alibaba.fastjson2.JSON.toJSONString(contextList);
            session.setContext(newContext);
            
            // 更新最后活跃时间和过期时间
            java.util.Date now = new java.util.Date();
            session.setLastActiveTime(now);
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.add(java.util.Calendar.MINUTE, SESSION_EXPIRE_MINUTES);
            session.setExpireTime(calendar.getTime());
            
            querySessionMapper.updateQuerySession(session);
            
            log.debug("更新会话上下文: sessionId={}, contextSize={}", sessionId, contextList.size());
            return true;
        }
        catch (Exception e)
        {
            log.error("更新会话上下文失败: sessionId={}", sessionId, e);
            return false;
        }
    }
    
    @Override
    public List<Map<String, String>> getContextForLLM(Long sessionId)
    {
        try
        {
            QuerySession session = querySessionMapper.selectQuerySessionById(sessionId);
            if (session == null || StringUtils.isEmpty(session.getContext()))
            {
                return Collections.emptyList();
            }
            
            return parseContext(session.getContext());
        }
        catch (Exception e)
        {
            log.error("获取会话上下文失败: sessionId={}", sessionId, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    @Scheduled(fixedRate = 600000) // 每10分钟执行一次
    public int clearExpiredSessions()
    {
        try
        {
            log.info("开始清理过期会话");
            int count = querySessionMapper.deleteExpiredSessions();
            log.info("清理过期会话完成: count={}", count);
            return count;
        }
        catch (Exception e)
        {
            log.error("清理过期会话失败", e);
            return 0;
        }
    }
    
    @Override
    public boolean deleteSession(Long sessionId)
    {
        try
        {
            int result = querySessionMapper.deleteQuerySessionById(sessionId);
            return result > 0;
        }
        catch (Exception e)
        {
            log.error("删除会话失败: sessionId={}", sessionId, e);
            return false;
        }
    }
    
    /**
     * 更新最后活跃时间
     */
    private void updateLastActiveTime(Long sessionId)
    {
        try
        {
            QuerySession session = querySessionMapper.selectQuerySessionById(sessionId);
            if (session != null)
            {
                java.util.Date now = new java.util.Date();
                session.setLastActiveTime(now);
                
                // 延长过期时间
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                calendar.add(java.util.Calendar.MINUTE, SESSION_EXPIRE_MINUTES);
                session.setExpireTime(calendar.getTime());
                
                querySessionMapper.updateQuerySession(session);
            }
        }
        catch (Exception e)
        {
            log.warn("更新最后活跃时间失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 解析上下文JSON
     */
    private List<Map<String, String>> parseContext(String contextJson)
    {
        if (StringUtils.isEmpty(contextJson))
        {
            return new ArrayList<>();
        }
        
        try
        {
            return com.alibaba.fastjson2.JSON.parseObject(
                contextJson,
                new com.alibaba.fastjson2.TypeReference<List<Map<String, String>>>() {}
            );
        }
        catch (Exception e)
        {
            log.warn("解析上下文JSON失败: context={}", contextJson, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 生成会话键
     */
    private String generateSessionKey()
    {
        return UUID.randomUUID().toString().replace("-", "") + "_" + System.currentTimeMillis();
    }
}
