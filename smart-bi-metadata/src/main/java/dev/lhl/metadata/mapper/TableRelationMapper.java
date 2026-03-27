package dev.lhl.metadata.mapper;

import dev.lhl.metadata.domain.TableRelation;
import java.util.List;

/**
 * 表推荐关系 Mapper
 *
 * @author smart-bi
 */
public interface TableRelationMapper {

    TableRelation selectById(Long id);

    List<TableRelation> selectList(TableRelation relation);

    int insert(TableRelation relation);

    int update(TableRelation relation);

    int deleteById(Long id);

    int deleteByIds(Long[] ids);
}
