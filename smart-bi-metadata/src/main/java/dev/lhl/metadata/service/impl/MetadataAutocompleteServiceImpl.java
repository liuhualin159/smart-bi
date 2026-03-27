package dev.lhl.metadata.service.impl;

import dev.lhl.common.utils.StringUtils;
import dev.lhl.metadata.domain.AtomicMetric;
import dev.lhl.metadata.domain.FieldMetadata;
import dev.lhl.metadata.domain.TableMetadata;
import dev.lhl.metadata.domain.dto.AutocompleteItem;
import dev.lhl.metadata.mapper.AtomicMetricMapper;
import dev.lhl.metadata.mapper.FieldMetadataMapper;
import dev.lhl.metadata.mapper.TableMetadataMapper;
import dev.lhl.metadata.service.IMetadataAutocompleteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 元数据自动补全服务实现
 * 在表、字段、指标上 LIKE 模糊搜索，按类型分组返回，最多 10 条
 */
@Service
public class MetadataAutocompleteServiceImpl implements IMetadataAutocompleteService
{
    private static final int DEFAULT_MAX = 10;

    @Autowired
    private TableMetadataMapper tableMetadataMapper;

    @Autowired
    private FieldMetadataMapper fieldMetadataMapper;

    @Autowired
    private AtomicMetricMapper atomicMetricMapper;

    @Override
    public List<AutocompleteItem> search(String keyword, Long userId, int maxResults)
    {
        List<AutocompleteItem> result = new ArrayList<>();
        if (maxResults <= 0)
        {
            maxResults = DEFAULT_MAX;
        }

        if (StringUtils.isEmpty(keyword) || keyword.trim().isEmpty())
        {
            return result;
        }

        String k = keyword.trim();
        int perType = Math.max(1, (maxResults + 2) / 3);

        List<TableMetadata> tables = tableMetadataMapper.selectByKeywordForAutocomplete(k, perType);
        if (tables != null)
        {
            for (TableMetadata t : tables)
            {
                if (result.size() >= maxResults) break;
                String label = t.getTableName();
                if (StringUtils.isNotEmpty(t.getTableComment()))
                {
                    label = label + "（" + t.getTableComment() + "）";
                }
                result.add(new AutocompleteItem("table", label, t.getTableName()));
            }
        }

        List<FieldMetadata> fields = fieldMetadataMapper.selectByKeywordForAutocomplete(k, perType);
        if (fields != null)
        {
            for (FieldMetadata f : fields)
            {
                if (result.size() >= maxResults) break;
                String label = f.getFieldName();
                if (StringUtils.isNotEmpty(f.getBusinessAlias()))
                {
                    label = label + " " + f.getBusinessAlias();
                }
                result.add(new AutocompleteItem("field", label, f.getFieldName()));
            }
        }

        List<AtomicMetric> metrics = atomicMetricMapper.selectByKeywordForAutocomplete(k, perType);
        if (metrics != null)
        {
            for (AtomicMetric m : metrics)
            {
                if (result.size() >= maxResults) break;
                String label = m.getName();
                if (StringUtils.isNotEmpty(m.getCode()))
                {
                    label = label + " [" + m.getCode() + "]";
                }
                result.add(new AutocompleteItem("metric", label, m.getName()));
            }
        }

        return result.size() > maxResults ? result.subList(0, maxResults) : result;
    }
}
