package dev.lhl.metadata.mapper;

import dev.lhl.metadata.domain.AtomicMetric;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 原子指标Mapper接口
 * 
 * @author smart-bi
 */
public interface AtomicMetricMapper
{
    public AtomicMetric selectAtomicMetricById(Long id);
    public List<AtomicMetric> selectAtomicMetricList(AtomicMetric atomicMetric);
    public int insertAtomicMetric(AtomicMetric atomicMetric);
    public int updateAtomicMetric(AtomicMetric atomicMetric);
    public int deleteAtomicMetricById(Long id);
    public int deleteAtomicMetricByIds(Long[] ids);

    /** 自动补全：按指标名/code 模糊搜索，限制条数 */
    List<AtomicMetric> selectByKeywordForAutocomplete(@Param("keyword") String keyword, @Param("limit") int limit);
}
