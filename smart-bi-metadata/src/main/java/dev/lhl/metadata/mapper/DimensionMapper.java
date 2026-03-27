package dev.lhl.metadata.mapper;

import dev.lhl.metadata.domain.Dimension;
import java.util.List;

/**
 * 维度Mapper接口
 * 
 * @author smart-bi
 */
public interface DimensionMapper
{
    public Dimension selectDimensionById(Long id);
    public List<Dimension> selectDimensionList(Dimension dimension);
    public int insertDimension(Dimension dimension);
    public int updateDimension(Dimension dimension);
    public int deleteDimensionById(Long id);
    public int deleteDimensionByIds(Long[] ids);
}
