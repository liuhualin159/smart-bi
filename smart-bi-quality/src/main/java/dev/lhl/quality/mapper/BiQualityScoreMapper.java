package dev.lhl.quality.mapper;

import dev.lhl.quality.domain.BiQualityScore;

import java.util.List;

/**
 * 数据质量评分 Mapper
 *
 * @author smart-bi
 */
public interface BiQualityScoreMapper {

    int insert(BiQualityScore record);

    List<BiQualityScore> selectByTableId(Long tableId);

    BiQualityScore selectLatestByTableAndType(Long tableId, String scoreType);
}
