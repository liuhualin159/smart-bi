package dev.lhl.query.service.impl;

import dev.lhl.metadata.domain.FieldMetadata;
import dev.lhl.metadata.domain.TableMetadata;
import dev.lhl.metadata.service.IMetadataService;
import dev.lhl.query.service.ITableMetadataToolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 元数据工具服务实现：供 LLM 获取全部表名与表结构，避免表名幻觉
 *
 * @author smart-bi
 */
@Service
public class TableMetadataToolServiceImpl implements ITableMetadataToolService
{
    private static final Logger log = LoggerFactory.getLogger(TableMetadataToolServiceImpl.class);

    @Autowired(required = false)
    private IMetadataService metadataService;

    @Override
    public List<Map<String, String>> listTablesWithComments()
    {
        List<Map<String, String>> result = new ArrayList<>();
        if (metadataService == null) return result;
        try
        {
            List<TableMetadata> tables = metadataService.selectTableMetadataListForNl2Sql(null);
            if (tables == null) return result;
            for (TableMetadata t : tables)
            {
                Map<String, String> row = new HashMap<>();
                row.put("table_name", t.getTableName() != null ? t.getTableName() : "");
                row.put("comment", t.getTableComment() != null ? t.getTableComment() : "");
                result.add(row);
            }
        }
        catch (Exception e)
        {
            log.warn("listTablesWithComments 失败", e);
        }
        return result;
    }

    @Override
    public String formatAllTablesListForPrompt()
    {
        List<Map<String, String>> list = listTablesWithComments();
        if (list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> row : list)
        {
            String name = row.getOrDefault("table_name", "").trim();
            String comment = row.getOrDefault("comment", "").trim();
            if (name.isEmpty()) continue;
            sb.append("- ").append(name);
            if (!comment.isEmpty()) sb.append("（").append(comment).append("）");
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public Map<String, Object> getTableSchema(String tableName)
    {
        if (tableName == null || tableName.trim().isEmpty() || metadataService == null)
            return null;
        try
        {
            List<TableMetadata> tables = metadataService.selectTableMetadataListForNl2Sql(null);
            if (tables == null) return null;
            TableMetadata target = null;
            String lower = tableName.trim().toLowerCase();
            for (TableMetadata t : tables)
            {
                if (t.getTableName() != null && t.getTableName().trim().toLowerCase().equals(lower))
                {
                    target = t;
                    break;
                }
            }
            if (target == null) return null;
            List<FieldMetadata> fields = metadataService.selectFieldMetadataListByTableId(target.getId());
            List<Map<String, String>> columns = new ArrayList<>();
            if (fields != null)
            {
                for (FieldMetadata f : fields)
                {
                    Map<String, String> col = new HashMap<>();
                    col.put("name", f.getFieldName() != null ? f.getFieldName() : "");
                    col.put("type", f.getFieldType() != null ? f.getFieldType() : "");
                    col.put("comment", f.getFieldComment() != null ? f.getFieldComment() : "");
                    if (f.getBusinessAlias() != null && !f.getBusinessAlias().isEmpty())
                        col.put("business_alias", f.getBusinessAlias());
                    columns.add(col);
                }
            }
            Map<String, Object> schema = new HashMap<>();
            schema.put("table_name", target.getTableName());
            schema.put("table_comment", target.getTableComment());
            schema.put("columns", columns);
            return schema;
        }
        catch (Exception e)
        {
            log.warn("getTableSchema 失败: tableName={}", tableName, e);
            return null;
        }
    }
}
