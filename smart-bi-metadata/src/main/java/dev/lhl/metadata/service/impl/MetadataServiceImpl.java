package dev.lhl.metadata.service.impl;

import dev.lhl.metadata.domain.*;
import dev.lhl.metadata.mapper.*;
import dev.lhl.metadata.service.IMetadataService;
import dev.lhl.metadata.service.VectorStoreService;
import dev.lhl.common.exception.ServiceException;
import dev.lhl.common.utils.DateUtils;
import dev.lhl.common.utils.SecurityUtils;
import dev.lhl.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;

/**
 * 元数据Service业务层处理
 * 
 * @author smart-bi
 */
@Service
public class MetadataServiceImpl implements IMetadataService
{
    private static final Logger log = LoggerFactory.getLogger(MetadataServiceImpl.class);

    @Autowired
    private BusinessDomainMapper businessDomainMapper;

    @Autowired
    private TableMetadataMapper tableMetadataMapper;

    @Autowired
    private FieldMetadataMapper fieldMetadataMapper;

    @Autowired
    private AtomicMetricMapper atomicMetricMapper;

    @Autowired
    private DimensionMapper dimensionMapper;

    @Autowired
    private FieldAliasMapper fieldAliasMapper;

    @Autowired(required = false)
    private TableRelationMapper tableRelationMapper;

    @Autowired
    private VectorStoreService vectorStoreService;

    // 业务域管理
    @Override
    public BusinessDomain selectBusinessDomainById(Long id)
    {
        return businessDomainMapper.selectBusinessDomainById(id);
    }

    @Override
    public List<BusinessDomain> selectBusinessDomainList(BusinessDomain businessDomain)
    {
        return businessDomainMapper.selectBusinessDomainList(businessDomain);
    }

    @Override
    @Transactional
    public int insertBusinessDomain(BusinessDomain businessDomain)
    {
        validateDescription(businessDomain.getDescription(), 1000, "业务域描述");
        businessDomain.setCreateBy(SecurityUtils.getUsername());
        businessDomain.setCreateTime(DateUtils.getNowDate());
        int result = businessDomainMapper.insertBusinessDomain(businessDomain);
        
        // 向量化业务域
        try
        {
            vectorizeBusinessDomain(businessDomain);
        }
        catch (Exception e)
        {
            log.warn("业务域向量化失败，但不影响数据保存: domainId={}, error={}", businessDomain.getId(), e.getMessage());
        }
        
        return result;
    }

    @Override
    @Transactional
    public int updateBusinessDomain(BusinessDomain businessDomain)
    {
        validateDescription(businessDomain.getDescription(), 1000, "业务域描述");
        businessDomain.setUpdateBy(SecurityUtils.getUsername());
        businessDomain.setUpdateTime(DateUtils.getNowDate());
        int result = businessDomainMapper.updateBusinessDomain(businessDomain);
        
        // 更新向量
        try
        {
            vectorizeBusinessDomain(businessDomain);
        }
        catch (Exception e)
        {
            log.warn("业务域向量更新失败，但不影响数据保存: domainId={}, error={}", businessDomain.getId(), e.getMessage());
        }
        
        return result;
    }

    @Override
    @Transactional
    public int deleteBusinessDomainByIds(Long[] ids)
    {
        // 删除向量
        try
        {
            for (Long id : ids)
            {
                vectorStoreService.delete("business_domain_" + id);
            }
        }
        catch (Exception e)
        {
            log.warn("业务域向量删除失败，但不影响数据删除: ids={}, error={}", ids, e.getMessage());
        }
        
        return businessDomainMapper.deleteBusinessDomainByIds(ids);
    }

    // 表元数据管理
    @Override
    public TableMetadata selectTableMetadataById(Long id)
    {
        return tableMetadataMapper.selectTableMetadataById(id);
    }

    @Override
    public TableMetadata selectTableMetadataByTableName(String tableName)
    {
        return tableName == null ? null : tableMetadataMapper.selectByTableName(tableName);
    }

    @Override
    public List<TableMetadata> selectTableMetadataList(TableMetadata tableMetadata)
    {
        return tableMetadataMapper.selectTableMetadataList(tableMetadata);
    }

    @Override
    @Transactional
    public int insertTableMetadata(TableMetadata tableMetadata)
    {
        validateDescription(tableMetadata.getBusinessDescription(), 2000, "表业务描述");
        tableMetadata.setCreateBy(SecurityUtils.getUsername());
        tableMetadata.setCreateTime(DateUtils.getNowDate());
        int result = tableMetadataMapper.insertTableMetadata(tableMetadata);
        // 向量化表元数据
        vectorizeTableMetadata(tableMetadata);
        return result;
    }

    @Override
    @Transactional
    public int updateTableMetadata(TableMetadata tableMetadata)
    {
        validateDescription(tableMetadata.getBusinessDescription(), 2000, "表业务描述");
        tableMetadata.setUpdateBy(SecurityUtils.getUsername());
        tableMetadata.setUpdateTime(DateUtils.getNowDate());
        int result = tableMetadataMapper.updateTableMetadata(tableMetadata);
        // 更新向量
        updateTableMetadataVector(tableMetadata);
        return result;
    }

    /**
     * 乐观锁单条更新（用途/可见性/粒度），用于行内编辑。冲突返回 0。
     */
    @Override
    @Transactional
    public int updateTableMetadataWithOptimisticLock(TableMetadata tableMetadata)
    {
        if (tableMetadata.getId() == null) return 0;
        tableMetadata.setUpdateBy(SecurityUtils.getUsername());
        return tableMetadataMapper.updateTableMetadataWithOptimisticLock(tableMetadata);
    }

    /**
     * 批量更新表的用途与可见性
     */
    @Override
    @Transactional
    public int batchUpdateTableMetadata(Long[] ids, String tableUsage, String nl2sqlVisibilityLevel)
    {
        if (ids == null || ids.length == 0) return 0;
        return tableMetadataMapper.batchUpdateTableMetadata(ids, tableUsage, nl2sqlVisibilityLevel);
    }

    @Override
    @Transactional
    public int deleteTableMetadataByIds(Long[] ids)
    {
        // 删除向量
        for (Long id : ids)
        {
            TableMetadata table = tableMetadataMapper.selectTableMetadataById(id);
            if (table != null && StringUtils.isNotEmpty(table.getVectorId()))
            {
                try
                {
                    vectorStoreService.delete(table.getVectorId());
                }
                catch (Exception e)
                {
                    log.error("删除表元数据向量失败: tableId={}, vectorId={}", id, table.getVectorId(), e);
                }
            }
        }
        return tableMetadataMapper.deleteTableMetadataByIds(ids);
    }

    @Override
    public List<TableMetadata> selectTableMetadataListForNl2Sql(List<Long> restrictToTableIds)
    {
        return tableMetadataMapper.selectTableMetadataListForNl2Sql(restrictToTableIds);
    }

    @Override
    public List<TableRelation> selectTableRelationList()
    {
        if (tableRelationMapper == null) return Collections.emptyList();
        return tableRelationMapper.selectList(new TableRelation());
    }

    @Override
    public List<TableRelation> selectTableRelationList(TableRelation relation)
    {
        if (tableRelationMapper == null) return Collections.emptyList();
        return tableRelationMapper.selectList(relation != null ? relation : new TableRelation());
    }

    @Override
    public TableRelation selectTableRelationById(Long id)
    {
        if (tableRelationMapper == null || id == null) return null;
        return tableRelationMapper.selectById(id);
    }

    @Override
    public int insertTableRelation(TableRelation relation)
    {
        if (tableRelationMapper == null || relation == null) return 0;
        if (relation.getCreateTime() == null)
            relation.setCreateTime(new java.util.Date());
        return tableRelationMapper.insert(relation);
    }

    @Override
    public int updateTableRelation(TableRelation relation)
    {
        if (tableRelationMapper == null || relation == null) return 0;
        return tableRelationMapper.update(relation);
    }

    @Override
    public int deleteTableRelationById(Long id)
    {
        if (tableRelationMapper == null || id == null) return 0;
        return tableRelationMapper.deleteById(id);
    }

    @Override
    public int deleteTableRelationByIds(Long[] ids)
    {
        if (tableRelationMapper == null || ids == null || ids.length == 0) return 0;
        return tableRelationMapper.deleteByIds(ids);
    }

    // 字段元数据管理
    @Override
    public FieldMetadata selectFieldMetadataById(Long id)
    {
        return fieldMetadataMapper.selectFieldMetadataById(id);
    }

    @Override
    public List<FieldMetadata> selectFieldMetadataList(FieldMetadata fieldMetadata)
    {
        return fieldMetadataMapper.selectFieldMetadataList(fieldMetadata);
    }

    @Override
    public List<FieldMetadata> selectFieldMetadataListByTableId(Long tableId)
    {
        return fieldMetadataMapper.selectFieldMetadataListByTableId(tableId);
    }

    @Override
    @Transactional
    public int insertFieldMetadata(FieldMetadata fieldMetadata)
    {
        validateDescription(fieldMetadata.getBusinessDescription(), 1000, "字段业务描述");
        fieldMetadata.setCreateBy(SecurityUtils.getUsername());
        fieldMetadata.setCreateTime(DateUtils.getNowDate());
        int result = fieldMetadataMapper.insertFieldMetadata(fieldMetadata);
        // 向量化字段元数据
        vectorizeFieldMetadata(fieldMetadata);
        return result;
    }

    @Override
    @Transactional
    public int updateFieldMetadata(FieldMetadata fieldMetadata)
    {
        validateDescription(fieldMetadata.getBusinessDescription(), 1000, "字段业务描述");
        fieldMetadata.setUpdateBy(SecurityUtils.getUsername());
        fieldMetadata.setUpdateTime(DateUtils.getNowDate());
        int result = fieldMetadataMapper.updateFieldMetadata(fieldMetadata);
        // 更新向量
        updateFieldMetadataVector(fieldMetadata);
        return result;
    }

    @Override
    @Transactional
    public int deleteFieldMetadataByIds(Long[] ids)
    {
        // 删除向量
        for (Long id : ids)
        {
            FieldMetadata field = fieldMetadataMapper.selectFieldMetadataById(id);
            if (field != null && StringUtils.isNotEmpty(field.getVectorId()))
            {
                try
                {
                    vectorStoreService.delete(field.getVectorId());
                }
                catch (Exception e)
                {
                    log.error("删除字段元数据向量失败: fieldId={}, vectorId={}", id, field.getVectorId(), e);
                }
            }
        }
        return fieldMetadataMapper.deleteFieldMetadataByIds(ids);
    }

    @Override
    @Transactional
    public int updateFieldMetadataWithOptimisticLock(FieldMetadata fieldMetadata)
    {
        if (fieldMetadata.getId() == null) return 0;
        fieldMetadata.setUpdateBy(SecurityUtils.getUsername());
        return fieldMetadataMapper.updateFieldMetadataWithOptimisticLock(fieldMetadata);
    }

    @Override
    public List<FieldAlias> selectFieldAliasListByFieldIds(List<Long> fieldIds)
    {
        if (fieldIds == null || fieldIds.isEmpty()) return Collections.emptyList();
        return fieldAliasMapper.selectByFieldIds(fieldIds);
    }

    @Override
    public List<FieldAlias> selectFieldAliasListByFieldId(Long fieldId)
    {
        if (fieldId == null) return Collections.emptyList();
        return fieldAliasMapper.selectByFieldId(fieldId);
    }

    @Override
    public FieldAlias selectFieldAliasByFieldIdAndAlias(Long fieldId, String alias)
    {
        if (fieldId == null || StringUtils.isEmpty(alias)) return null;
        return fieldAliasMapper.selectByFieldIdAndAlias(fieldId, alias.trim());
    }

    @Override
    @Transactional
    public FieldAlias insertFieldAlias(FieldAlias fieldAlias)
    {
        if (fieldAlias.getFieldId() == null || StringUtils.isEmpty(fieldAlias.getAlias()))
            throw new ServiceException("字段ID与别名不能为空");
        String alias = fieldAlias.getAlias().trim();
        if (fieldAliasMapper.selectByFieldIdAndAlias(fieldAlias.getFieldId(), alias) != null)
            throw new ServiceException("该字段下已存在相同别名，请勿重复添加");
        fieldAlias.setAlias(alias);
        fieldAlias.setCreateBy(SecurityUtils.getUsername());
        fieldAlias.setCreateTime(DateUtils.getNowDate());
        if (StringUtils.isEmpty(fieldAlias.getSource())) fieldAlias.setSource("HUMAN");
        fieldAliasMapper.insert(fieldAlias);
        return fieldAlias;
    }

    @Override
    @Transactional
    public int deleteFieldAliasById(Long id)
    {
        if (id == null) return 0;
        return fieldAliasMapper.deleteById(id);
    }

    @Override
    public FieldAlias selectFieldAliasById(Long id)
    {
        if (id == null) return null;
        return fieldAliasMapper.selectById(id);
    }

    @Override
    public List<AliasConflictItem> findAliasConflicts(String alias, Long excludeFieldId)
    {
        if (StringUtils.isEmpty(alias)) return Collections.emptyList();
        return fieldAliasMapper.selectConflictItems(alias.trim(), excludeFieldId);
    }

    @Override
    public List<Map<String, String>> getFieldAliasSuggestions(Long fieldId)
    {
        if (fieldId == null) return Collections.emptyList();
        // MVP: 返回空；后续可从 bi_llm_audit 解析 generated_sql 提取常见命名
        return Collections.emptyList();
    }

    // 原子指标管理
    @Override
    public AtomicMetric selectAtomicMetricById(Long id)
    {
        return atomicMetricMapper.selectAtomicMetricById(id);
    }

    @Override
    public List<AtomicMetric> selectAtomicMetricList(AtomicMetric atomicMetric)
    {
        return atomicMetricMapper.selectAtomicMetricList(atomicMetric);
    }

    @Override
    @Transactional
    public int insertAtomicMetric(AtomicMetric atomicMetric)
    {
        atomicMetric.setCreateBy(SecurityUtils.getUsername());
        atomicMetric.setCreateTime(DateUtils.getNowDate());
        if (atomicMetric.getCode() == null || atomicMetric.getCode().trim().isEmpty())
        {
            atomicMetric.setCode(generateMetricCode(atomicMetric.getDomainId()));
        }
        int result = atomicMetricMapper.insertAtomicMetric(atomicMetric);
        
        // 向量化指标
        try
        {
            vectorizeAtomicMetric(atomicMetric);
        }
        catch (Exception e)
        {
            log.warn("指标向量化失败，但不影响数据保存: metricId={}, error={}", atomicMetric.getId(), e.getMessage());
        }
        
        return result;
    }

    @Override
    @Transactional
    public int updateAtomicMetric(AtomicMetric atomicMetric)
    {
        atomicMetric.setUpdateBy(SecurityUtils.getUsername());
        atomicMetric.setUpdateTime(DateUtils.getNowDate());
        int result = atomicMetricMapper.updateAtomicMetric(atomicMetric);
        
        // 更新向量
        try
        {
            updateAtomicMetricVector(atomicMetric);
        }
        catch (Exception e)
        {
            log.warn("指标向量更新失败，但不影响数据保存: metricId={}, error={}", atomicMetric.getId(), e.getMessage());
        }
        
        return result;
    }

    @Override
    @Transactional
    public int deleteAtomicMetricByIds(Long[] ids)
    {
        // 删除向量
        try
        {
            for (Long id : ids)
            {
                vectorStoreService.delete("atomic_metric_" + id);
            }
        }
        catch (Exception e)
        {
            log.warn("指标向量删除失败，但不影响数据删除: ids={}, error={}", ids, e.getMessage());
        }
        
        return atomicMetricMapper.deleteAtomicMetricByIds(ids);
    }

    // 维度管理
    @Override
    public Dimension selectDimensionById(Long id)
    {
        return dimensionMapper.selectDimensionById(id);
    }

    @Override
    public List<Dimension> selectDimensionList(Dimension dimension)
    {
        return dimensionMapper.selectDimensionList(dimension);
    }

    @Override
    @Transactional
    public int insertDimension(Dimension dimension)
    {
        dimension.setCreateBy(SecurityUtils.getUsername());
        dimension.setCreateTime(DateUtils.getNowDate());
        return dimensionMapper.insertDimension(dimension);
    }

    @Override
    @Transactional
    public int updateDimension(Dimension dimension)
    {
        dimension.setUpdateBy(SecurityUtils.getUsername());
        dimension.setUpdateTime(DateUtils.getNowDate());
        return dimensionMapper.updateDimension(dimension);
    }

    @Override
    @Transactional
    public int deleteDimensionByIds(Long[] ids)
    {
        return dimensionMapper.deleteDimensionByIds(ids);
    }

    // 向量化方法
    private void vectorizeTableMetadata(TableMetadata tableMetadata)
    {
        try
        {
            String text = buildTableMetadataText(tableMetadata);
            String vectorId = "table_" + tableMetadata.getId();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "table");
            metadata.put("tableId", tableMetadata.getId());
            metadata.put("tableName", tableMetadata.getTableName());
            metadata.put("domainId", tableMetadata.getDomainId());
            
            vectorStoreService.store(vectorId, text, metadata);
            tableMetadata.setVectorId(vectorId);
            tableMetadataMapper.updateTableMetadata(tableMetadata);
            
            log.info("表元数据向量化成功: tableId={}, vectorId={}", tableMetadata.getId(), vectorId);
        }
        catch (Exception e)
        {
            log.error("表元数据向量化失败: tableId={}", tableMetadata.getId(), e);
            // 记录日志，保留旧向量，支持重试
        }
    }

    private void updateTableMetadataVector(TableMetadata tableMetadata)
    {
        try
        {
            if (StringUtils.isEmpty(tableMetadata.getVectorId()))
            {
                vectorizeTableMetadata(tableMetadata);
            }
            else
            {
                String text = buildTableMetadataText(tableMetadata);
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("type", "table");
                metadata.put("tableId", tableMetadata.getId());
                metadata.put("tableName", tableMetadata.getTableName());
                metadata.put("domainId", tableMetadata.getDomainId());
                
                vectorStoreService.update(tableMetadata.getVectorId(), text, metadata);
                log.info("表元数据向量更新成功: tableId={}, vectorId={}", tableMetadata.getId(), tableMetadata.getVectorId());
            }
        }
        catch (Exception e)
        {
            log.error("表元数据向量更新失败: tableId={}", tableMetadata.getId(), e);
            // 记录日志，保留旧向量，支持重试
        }
    }

    private void vectorizeFieldMetadata(FieldMetadata fieldMetadata)
    {
        try
        {
            String text = buildFieldMetadataText(fieldMetadata);
            String vectorId = "field_" + fieldMetadata.getId();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "field");
            metadata.put("fieldId", fieldMetadata.getId());
            metadata.put("fieldName", fieldMetadata.getFieldName());
            metadata.put("tableId", fieldMetadata.getTableId());
            
            vectorStoreService.store(vectorId, text, metadata);
            fieldMetadata.setVectorId(vectorId);
            fieldMetadataMapper.updateFieldMetadata(fieldMetadata);
            
            log.info("字段元数据向量化成功: fieldId={}, vectorId={}", fieldMetadata.getId(), vectorId);
        }
        catch (Exception e)
        {
            log.error("字段元数据向量化失败: fieldId={}", fieldMetadata.getId(), e);
            // 记录日志，保留旧向量，支持重试
        }
    }

    private void updateFieldMetadataVector(FieldMetadata fieldMetadata)
    {
        try
        {
            if (StringUtils.isEmpty(fieldMetadata.getVectorId()))
            {
                vectorizeFieldMetadata(fieldMetadata);
            }
            else
            {
                String text = buildFieldMetadataText(fieldMetadata);
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("type", "field");
                metadata.put("fieldId", fieldMetadata.getId());
                metadata.put("fieldName", fieldMetadata.getFieldName());
                metadata.put("tableId", fieldMetadata.getTableId());
                
                vectorStoreService.update(fieldMetadata.getVectorId(), text, metadata);
                log.info("字段元数据向量更新成功: fieldId={}, vectorId={}", fieldMetadata.getId(), fieldMetadata.getVectorId());
            }
        }
        catch (Exception e)
        {
            log.error("字段元数据向量更新失败: fieldId={}", fieldMetadata.getId(), e);
            // 记录日志，保留旧向量，支持重试
        }
    }

    private String buildTableMetadataText(TableMetadata table)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("表名: ").append(table.getTableName()).append("\n");
        if (StringUtils.isNotEmpty(table.getTableComment()))
        {
            sb.append("表注释: ").append(table.getTableComment()).append("\n");
        }
        if (StringUtils.isNotEmpty(table.getBusinessDescription()))
        {
            sb.append("业务描述: ").append(table.getBusinessDescription()).append("\n");
        }
        return sb.toString();
    }

    private String buildFieldMetadataText(FieldMetadata field)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("字段名: ").append(field.getFieldName()).append("\n");
        if (StringUtils.isNotEmpty(field.getBusinessAlias()))
        {
            sb.append("业务别名: ").append(field.getBusinessAlias()).append("\n");
        }
        if (StringUtils.isNotEmpty(field.getFieldComment()))
        {
            sb.append("字段注释: ").append(field.getFieldComment()).append("\n");
        }
        if (StringUtils.isNotEmpty(field.getBusinessDescription()))
        {
            sb.append("业务描述: ").append(field.getBusinessDescription()).append("\n");
        }
        return sb.toString();
    }

    private void validateDescription(String description, int maxLength, String fieldName)
    {
        if (StringUtils.isNotEmpty(description) && description.length() > maxLength)
        {
            throw new RuntimeException(fieldName + "长度不能超过" + maxLength + "字");
        }
    }
    
    /**
     * 向量化业务域
     */
    private void vectorizeBusinessDomain(BusinessDomain businessDomain)
    {
        try
        {
            String text = buildBusinessDomainText(businessDomain);
            String vectorId = "business_domain_" + businessDomain.getId();
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "business_domain");
            metadata.put("domainId", businessDomain.getId());
            metadata.put("domainCode", businessDomain.getCode());
            metadata.put("domainName", businessDomain.getName());
            
            vectorStoreService.store(vectorId, text, metadata);
            log.info("业务域向量化成功: domainId={}, vectorId={}", businessDomain.getId(), vectorId);
        }
        catch (Exception e)
        {
            log.error("业务域向量化失败: domainId={}", businessDomain.getId(), e);
            // 记录日志，但不影响业务
        }
    }
    
    /**
     * 构建业务域文本
     */
    private String buildBusinessDomainText(BusinessDomain domain)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("业务域名称: ").append(domain.getName()).append("\n");
        if (StringUtils.isNotEmpty(domain.getCode()))
        {
            sb.append("业务域编码: ").append(domain.getCode()).append("\n");
        }
        if (StringUtils.isNotEmpty(domain.getDescription()))
        {
            sb.append("业务域描述: ").append(domain.getDescription()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * 自动生成指标编码，格式：M{domainId}_{8位hex}
     */
    private String generateMetricCode(Long domainId)
    {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "M" + (domainId != null ? domainId : 0) + "_" + suffix;
    }

    /**
     * 向量化指标
     */
    private void vectorizeAtomicMetric(AtomicMetric atomicMetric)
    {
        try
        {
            String text = buildAtomicMetricText(atomicMetric);
            String vectorId = "atomic_metric_" + atomicMetric.getId();
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "atomic_metric");
            metadata.put("metricId", atomicMetric.getId());
            metadata.put("metricCode", atomicMetric.getCode());
            metadata.put("metricName", atomicMetric.getName());
            if (atomicMetric.getDomainId() != null)
            {
                metadata.put("domainId", atomicMetric.getDomainId());
            }
            
            vectorStoreService.store(vectorId, text, metadata);
            log.info("指标向量化成功: metricId={}, vectorId={}", atomicMetric.getId(), vectorId);
        }
        catch (Exception e)
        {
            log.error("指标向量化失败: metricId={}", atomicMetric.getId(), e);
            // 记录日志，但不影响业务
        }
    }
    
    /**
     * 更新指标向量
     */
    private void updateAtomicMetricVector(AtomicMetric atomicMetric)
    {
        try
        {
            String text = buildAtomicMetricText(atomicMetric);
            String vectorId = "atomic_metric_" + atomicMetric.getId();
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "atomic_metric");
            metadata.put("metricId", atomicMetric.getId());
            metadata.put("metricCode", atomicMetric.getCode());
            metadata.put("metricName", atomicMetric.getName());
            if (atomicMetric.getDomainId() != null)
            {
                metadata.put("domainId", atomicMetric.getDomainId());
            }
            
            vectorStoreService.update(vectorId, text, metadata);
            log.info("指标向量更新成功: metricId={}, vectorId={}", atomicMetric.getId(), vectorId);
        }
        catch (Exception e)
        {
            log.error("指标向量更新失败: metricId={}", atomicMetric.getId(), e);
            // 记录日志，但不影响业务
        }
    }
    
    /**
     * 构建指标文本
     */
    private String buildAtomicMetricText(AtomicMetric metric)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("指标名称: ").append(metric.getName()).append("\n");
        if (StringUtils.isNotEmpty(metric.getCode()))
        {
            sb.append("指标编码: ").append(metric.getCode()).append("\n");
        }
        if (StringUtils.isNotEmpty(metric.getExpression()))
        {
            sb.append("指标表达式: ").append(metric.getExpression()).append("\n");
        }
        if (StringUtils.isNotEmpty(metric.getDescription()))
        {
            sb.append("指标描述: ").append(metric.getDescription()).append("\n");
        }
        return sb.toString();
    }
}
