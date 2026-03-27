package dev.lhl.query.service.impl;

import dev.lhl.common.core.domain.entity.SysUser;
import dev.lhl.common.utils.DesensitizeUtils;
import dev.lhl.common.utils.SecurityUtils;
import dev.lhl.common.utils.StringUtils;
import dev.lhl.metadata.domain.FieldMetadata;
import dev.lhl.metadata.service.IMetadataService;
import dev.lhl.system.service.IDataPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 敏感字段脱敏服务
 * 负责对查询结果中的敏感字段进行脱敏处理
 * 
 * @author smart-bi
 */
@Service
public class DesensitizeService
{
    private static final Logger log = LoggerFactory.getLogger(DesensitizeService.class);
    
    @Autowired(required = false)
    private IDataPermissionService dataPermissionService;
    
    @Autowired(required = false)
    private IMetadataService metadataService;
    
    /**
     * 对查询结果进行脱敏处理
     * 
     * @param results 查询结果（List<Map<String, Object>>）
     * @param tableName 表名
     * @param userId 用户ID
     * @return 脱敏后的查询结果
     */
    public List<Map<String, Object>> desensitizeResults(List<Map<String, Object>> results, String tableName, Long userId)
    {
        if (results == null || results.isEmpty() || StringUtils.isEmpty(tableName) || userId == null)
        {
            return results;
        }
        
        try
        {
            log.debug("开始脱敏处理: tableName={}, userId={}, resultCount={}", tableName, userId, results.size());
            
            // 检查用户是否为管理员（管理员不需要脱敏）
            SysUser user = SecurityUtils.getLoginUser().getUser();
            if (user != null && user.isAdmin())
            {
                log.debug("管理员用户，跳过脱敏处理");
                return results;
            }
            
            // 获取表的字段元数据，识别敏感字段
            Map<String, FieldMetadata> sensitiveFields = getSensitiveFields(tableName);
            if (sensitiveFields.isEmpty())
            {
                log.debug("未找到敏感字段，跳过脱敏处理");
                return results;
            }
            
            // 对每条记录进行脱敏处理
            List<Map<String, Object>> desensitizedResults = new ArrayList<>();
            for (Map<String, Object> row : results)
            {
                Map<String, Object> desensitizedRow = new HashMap<>(row);
                
                for (Map.Entry<String, Object> entry : row.entrySet())
                {
                    String fieldName = entry.getKey();
                    Object fieldValue = entry.getValue();
                    
                    // 检查字段是否为敏感字段
                    FieldMetadata fieldMetadata = sensitiveFields.get(fieldName);
                    if (fieldMetadata != null && fieldMetadata.getIsSensitive() != null && fieldMetadata.getIsSensitive())
                    {
                        // 检查用户是否有字段访问权限
                        if (dataPermissionService != null)
                        {
                            boolean hasFieldPermission = dataPermissionService.checkFieldPermission(userId, tableName, fieldName);
                            if (!hasFieldPermission)
                            {
                                // 无权限，进行脱敏
                                String desensitizedValue = desensitizeField(fieldValue, fieldMetadata);
                                desensitizedRow.put(fieldName, desensitizedValue);
                                log.debug("字段已脱敏（无权限）: tableName={}, fieldName={}, userId={}", tableName, fieldName, userId);
                            }
                            // 有权限，保留原始值
                        }
                        else
                        {
                            // 数据权限服务未配置，默认脱敏敏感字段
                            String desensitizedValue = desensitizeField(fieldValue, fieldMetadata);
                            desensitizedRow.put(fieldName, desensitizedValue);
                            log.debug("字段已脱敏（默认）: tableName={}, fieldName={}, userId={}", tableName, fieldName, userId);
                        }
                    }
                }
                
                desensitizedResults.add(desensitizedRow);
            }
            
            log.debug("脱敏处理完成: tableName={}, userId={}, desensitizedCount={}", tableName, userId, desensitizedResults.size());
            return desensitizedResults;
        }
        catch (Exception e)
        {
            log.error("脱敏处理失败，返回原始结果: tableName={}, userId={}", tableName, userId, e);
            return results;
        }
    }
    
    /**
     * 获取表的敏感字段元数据
     */
    private Map<String, FieldMetadata> getSensitiveFields(String tableName)
    {
        Map<String, FieldMetadata> sensitiveFields = new HashMap<>();
        
        try
        {
            if (metadataService != null)
            {
                // 先根据表名查询表元数据
                dev.lhl.metadata.domain.TableMetadata tableQuery = new dev.lhl.metadata.domain.TableMetadata();
                tableQuery.setTableName(tableName);
                List<dev.lhl.metadata.domain.TableMetadata> tables = metadataService.selectTableMetadataList(tableQuery);
                
                if (tables != null && !tables.isEmpty())
                {
                    // 使用第一个匹配的表
                    Long tableId = tables.get(0).getId();
                    
                    // 查询表的字段元数据
                    List<FieldMetadata> fields = metadataService.selectFieldMetadataListByTableId(tableId);
                    for (FieldMetadata field : fields)
                    {
                        if (field.getIsSensitive() != null && field.getIsSensitive())
                        {
                            sensitiveFields.put(field.getFieldName(), field);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.warn("获取敏感字段元数据失败: tableName={}", tableName, e);
        }
        
        return sensitiveFields;
    }
    
    /**
     * 对单个字段值进行脱敏
     */
    private String desensitizeField(Object fieldValue, FieldMetadata fieldMetadata)
    {
        if (fieldValue == null)
        {
            return null;
        }
        
        String value = fieldValue.toString();
        if (StringUtils.isEmpty(value))
        {
            return value;
        }
        
        try
        {
            // 如果有自定义脱敏规则，使用自定义规则
            String desensitizeRule = fieldMetadata.getDesensitizeRule();
            if (StringUtils.isNotEmpty(desensitizeRule))
            {
                return applyCustomRule(value, desensitizeRule);
            }
            
            // 根据字段类型自动选择脱敏方法
            String fieldType = fieldMetadata.getFieldType();
            if (StringUtils.isNotEmpty(fieldType))
            {
                String upperType = fieldType.toUpperCase();
                
                // 手机号
                if (upperType.contains("PHONE") || upperType.contains("MOBILE") || 
                    value.matches("^1[3-9]\\d{9}$"))
                {
                    return DesensitizeUtils.desensitizePhone(value);
                }
                
                // 身份证号
                if (upperType.contains("ID_CARD") || upperType.contains("IDCARD") ||
                    (value.length() == 18 && value.matches("^\\d{17}[\\dXx]$")))
                {
                    return DesensitizeUtils.desensitizeIdCard(value);
                }
                
                // 邮箱
                if (upperType.contains("EMAIL") || value.contains("@"))
                {
                    return DesensitizeUtils.desensitizeEmail(value);
                }
                
                // 银行卡号
                if (upperType.contains("BANK_CARD") || upperType.contains("CARD") ||
                    (value.length() >= 16 && value.matches("^\\d+$")))
                {
                    return DesensitizeUtils.desensitizeBankCard(value);
                }
            }
            
            // 默认脱敏：保留前后各2位
            return DesensitizeUtils.desensitize(value, 2, 2);
        }
        catch (Exception e)
        {
            log.warn("字段脱敏失败，返回默认值: fieldValue={}", fieldValue, e);
            return "***";
        }
    }
    
    /**
     * 应用自定义脱敏规则
     * 规则格式：prefix:suffix 或 prefixN:suffixN（N为保留位数）
     * 例如：3:4 表示保留前3位和后4位
     */
    private String applyCustomRule(String value, String rule)
    {
        if (StringUtils.isEmpty(rule) || !rule.contains(":"))
        {
            // 无效规则，使用默认脱敏
            return DesensitizeUtils.desensitize(value, 2, 2);
        }
        
        try
        {
            String[] parts = rule.split(":");
            if (parts.length != 2)
            {
                return DesensitizeUtils.desensitize(value, 2, 2);
            }
            
            int prefixLength = Integer.parseInt(parts[0].trim());
            int suffixLength = Integer.parseInt(parts[1].trim());
            
            return DesensitizeUtils.desensitize(value, prefixLength, suffixLength);
        }
        catch (Exception e)
        {
            log.warn("应用自定义脱敏规则失败，使用默认规则: value={}, rule={}", value, rule, e);
            return DesensitizeUtils.desensitize(value, 2, 2);
        }
    }
}
