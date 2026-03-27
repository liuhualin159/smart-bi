package dev.lhl.query.mapper;

import dev.lhl.query.domain.QuerySession;
import java.util.List;

/**
 * 查询会话Mapper接口
 * 
 * @author smart-bi
 */
public interface QuerySessionMapper
{
    QuerySession selectQuerySessionById(Long id);
    QuerySession selectQuerySessionBySessionKey(String sessionKey);
    QuerySession selectQuerySessionByUserIdAndSessionKey(Long userId, String sessionKey);
    List<QuerySession> selectQuerySessionList(QuerySession querySession);
    int insertQuerySession(QuerySession querySession);
    int updateQuerySession(QuerySession querySession);
    int deleteQuerySessionById(Long id);
    int deleteExpiredSessions();
}
