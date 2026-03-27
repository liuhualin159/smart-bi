package dev.lhl.metadata.mapper;

import dev.lhl.metadata.domain.FieldAlias;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 字段别名 Mapper
 *
 * @author smart-bi
 */
public interface FieldAliasMapper {

    FieldAlias selectById(Long id);

    List<FieldAlias> selectByFieldId(Long fieldId);

    int insert(FieldAlias fieldAlias);

    int update(FieldAlias fieldAlias);

    int deleteById(Long id);

    int deleteByFieldId(Long fieldId);

    /** 检查 (field_id, alias) 是否已存在 */
    FieldAlias selectByFieldIdAndAlias(@Param("fieldId") Long fieldId, @Param("alias") String alias);

    /** 按多个字段 ID 查询别名列表（用于批量组装 VO） */
    List<FieldAlias> selectByFieldIds(@Param("fieldIds") List<Long> fieldIds);

    /** 按别名查询所有记录（用于冲突检测） */
    List<FieldAlias> selectByAlias(@Param("alias") String alias);

    /** 别名冲突：同一 alias 对应的其他表/字段（excludeFieldId 可选排除当前字段） */
    List<dev.lhl.metadata.domain.AliasConflictItem> selectConflictItems(@Param("alias") String alias, @Param("excludeFieldId") Long excludeFieldId);
}
