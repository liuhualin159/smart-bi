package dev.lhl.metadata.service;

import dev.lhl.metadata.domain.*;
import java.util.List;

/**
 * 元数据Service接口
 * 
 * @author smart-bi
 */
public interface IMetadataService
{
    // 业务域管理
    BusinessDomain selectBusinessDomainById(Long id);
    List<BusinessDomain> selectBusinessDomainList(BusinessDomain businessDomain);
    int insertBusinessDomain(BusinessDomain businessDomain);
    int updateBusinessDomain(BusinessDomain businessDomain);
    int deleteBusinessDomainByIds(Long[] ids);

    // 表元数据管理
    TableMetadata selectTableMetadataById(Long id);
    TableMetadata selectTableMetadataByTableName(String tableName);
    List<TableMetadata> selectTableMetadataList(TableMetadata tableMetadata);
    int insertTableMetadata(TableMetadata tableMetadata);
    int updateTableMetadata(TableMetadata tableMetadata);
    /** 乐观锁单条更新（用途/可见性/粒度），返回影响行数，0 表示版本冲突 */
    int updateTableMetadataWithOptimisticLock(TableMetadata tableMetadata);
    /** 批量更新表的用途与可见性（按 ids） */
    int batchUpdateTableMetadata(Long[] ids, String tableUsage, String nl2sqlVisibilityLevel);
    int deleteTableMetadataByIds(Long[] ids);

    /** NL2SQL 白名单表：仅 nl2sql_visibility_level in (NORMAL,PREFERRED)；restrictToTableIds 为空则全部，否则取交集 */
    List<TableMetadata> selectTableMetadataListForNl2Sql(List<Long> restrictToTableIds);

    /** 推荐 join 列表（供 NL2SQL 提示词） */
    List<TableRelation> selectTableRelationList();

    /** 表关系（推荐 join）分页列表，支持按左表/右表筛选 */
    List<TableRelation> selectTableRelationList(TableRelation relation);
    TableRelation selectTableRelationById(Long id);
    int insertTableRelation(TableRelation relation);
    int updateTableRelation(TableRelation relation);
    int deleteTableRelationById(Long id);
    int deleteTableRelationByIds(Long[] ids);

    // 字段元数据管理
    FieldMetadata selectFieldMetadataById(Long id);
    List<FieldMetadata> selectFieldMetadataList(FieldMetadata fieldMetadata);
    List<FieldMetadata> selectFieldMetadataListByTableId(Long tableId);
    /** 按字段 ID 列表批量查询别名（用于组装 VO） */
    List<FieldAlias> selectFieldAliasListByFieldIds(List<Long> fieldIds);
    int insertFieldMetadata(FieldMetadata fieldMetadata);
    int updateFieldMetadata(FieldMetadata fieldMetadata);
    /** 乐观锁单条更新字段（用途/语义类型/单位/聚合/优先级/敏感/曝光），返回影响行数，0 表示冲突 */
    int updateFieldMetadataWithOptimisticLock(FieldMetadata fieldMetadata);
    int deleteFieldMetadataByIds(Long[] ids);

    // 字段别名
    List<FieldAlias> selectFieldAliasListByFieldId(Long fieldId);
    FieldAlias selectFieldAliasByFieldIdAndAlias(Long fieldId, String alias);
    FieldAlias insertFieldAlias(FieldAlias fieldAlias);
    int deleteFieldAliasById(Long id);
    FieldAlias selectFieldAliasById(Long id);

    /** 查询别名冲突：同一 alias 出现在其他表/字段的列表，excludeFieldId 可排除当前字段 */
    List<AliasConflictItem> findAliasConflicts(String alias, Long excludeFieldId);

    /** 推荐别名列表（供前端采纳），来源可为历史 SQL/审计；MVP 可返回空 */
    List<java.util.Map<String, String>> getFieldAliasSuggestions(Long fieldId);

    // 原子指标管理
    AtomicMetric selectAtomicMetricById(Long id);
    List<AtomicMetric> selectAtomicMetricList(AtomicMetric atomicMetric);
    int insertAtomicMetric(AtomicMetric atomicMetric);
    int updateAtomicMetric(AtomicMetric atomicMetric);
    int deleteAtomicMetricByIds(Long[] ids);

    // 维度管理
    Dimension selectDimensionById(Long id);
    List<Dimension> selectDimensionList(Dimension dimension);
    int insertDimension(Dimension dimension);
    int updateDimension(Dimension dimension);
    int deleteDimensionByIds(Long[] ids);
}
