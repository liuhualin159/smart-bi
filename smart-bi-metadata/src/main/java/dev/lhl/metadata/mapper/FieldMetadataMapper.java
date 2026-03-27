package dev.lhl.metadata.mapper;

import dev.lhl.metadata.domain.FieldMetadata;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 字段元数据Mapper接口
 * 
 * @author smart-bi
 */
public interface FieldMetadataMapper
{
    public FieldMetadata selectFieldMetadataById(Long id);
    public List<FieldMetadata> selectFieldMetadataList(FieldMetadata fieldMetadata);
    public List<FieldMetadata> selectFieldMetadataListByTableId(Long tableId);
    public int insertFieldMetadata(FieldMetadata fieldMetadata);
    public int updateFieldMetadata(FieldMetadata fieldMetadata);
    /** 乐观锁更新字段（用途/语义/单位/聚合/优先级/敏感/曝光），返回影响行数 */
    int updateFieldMetadataWithOptimisticLock(FieldMetadata fieldMetadata);
    public int deleteFieldMetadataById(Long id);
    public int deleteFieldMetadataByIds(Long[] ids);

    /** 自动补全：按字段名/业务别名模糊搜索，限制条数 */
    List<FieldMetadata> selectByKeywordForAutocomplete(@Param("keyword") String keyword, @Param("limit") int limit);
}
