package dev.lhl.query.service.impl;

import dev.lhl.query.domain.QueryRecord;
import dev.lhl.query.mapper.QueryRecordMapper;
import dev.lhl.query.service.ISlowQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 慢查询监控服务实现
 * 基于 bi_query_record 中 execution_time 超过阈值的记录
 *
 * @author smart-bi
 */
@Service
public class SlowQueryServiceImpl implements ISlowQueryService {

    @Autowired(required = false)
    private QueryRecordMapper queryRecordMapper;

    @Override
    public List<Map<String, Object>> getSlowQueries(long thresholdMs, int limit) {
        if (queryRecordMapper == null) return Collections.emptyList();
        List<QueryRecord> list = queryRecordMapper.selectSlowQueryRecords(thresholdMs, limit);
        if (list == null) return Collections.emptyList();
        return list.stream().map(this::toMap).toList();
    }

    private Map<String, Object> toMap(QueryRecord r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("question", r.getQuestion());
        m.put("executedSql", r.getExecutedSql());
        m.put("duration", r.getDuration());
        m.put("createTime", r.getCreateTime());
        m.put("userId", r.getUserId());
        return m;
    }
}
