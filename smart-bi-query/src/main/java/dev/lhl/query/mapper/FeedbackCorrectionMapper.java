package dev.lhl.query.mapper;

import dev.lhl.query.domain.FeedbackCorrection;
import java.util.List;

/**
 * 反馈修正 Mapper 接口
 *
 * @author smart-bi
 */
public interface FeedbackCorrectionMapper {

    FeedbackCorrection selectById(Long id);
    List<FeedbackCorrection> selectByStatus(String status);
    int insert(FeedbackCorrection record);
    int updateById(FeedbackCorrection record);
}
