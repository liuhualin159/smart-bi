package dev.lhl.query.service.impl;

import dev.lhl.datasource.service.IDataSourceService;
import dev.lhl.metadata.domain.TableMetadata;
import dev.lhl.metadata.service.IMetadataService;
import dev.lhl.query.service.ITablePreviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 表数据预览服务实现
 *
 * @author smart-bi
 */
@Service
public class TablePreviewServiceImpl implements ITablePreviewService {

    private static final Logger log = LoggerFactory.getLogger(TablePreviewServiceImpl.class);
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;
    private static final int QUERY_TIMEOUT_SECONDS = 30;
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    @Autowired
    private IMetadataService metadataService;

    @Autowired
    private IDataSourceService dataSourceService;

    @Override
    public PreviewResult previewTable(Long tableId, int limit, Long userId) {
        if (tableId == null) {
            throw new IllegalArgumentException("tableId 不能为空");
        }

        TableMetadata table = metadataService.selectTableMetadataById(tableId);
        if (table == null) {
            throw new IllegalArgumentException("表不存在: id=" + tableId);
        }

        String tableName = table.getTableName();
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("表名不能为空");
        }

        if ("HIDDEN".equals(table.getNl2sqlVisibilityLevel())) {
            throw new SecurityException("该表不可预览");
        }

        if (!TABLE_NAME_PATTERN.matcher(tableName.trim()).matches()) {
            throw new IllegalArgumentException("表名包含非法字符");
        }

        int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        String sql = "SELECT * FROM `" + tableName.trim() + "` LIMIT " + safeLimit;

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = dataSourceService.getLocalConnection();
            stmt = conn.createStatement();
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            rs = stmt.executeQuery(sql);

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<String> columns = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnLabel(i));
            }

            List<Map<String, Object>> data = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(columns.get(i - 1), rs.getObject(i));
                }
                data.add(row);
            }

            log.debug("表预览完成: tableName={}, rows={}", tableName, data.size());
            return new PreviewResult(columns, data);
        } catch (Exception e) {
            log.warn("表预览失败: tableId={}, tableName={}", tableId, tableName, e);
            throw new RuntimeException("预览失败: " + e.getMessage());
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) { log.debug("关闭ResultSet", e); }
            if (stmt != null) try { stmt.close(); } catch (Exception e) { log.debug("关闭Statement", e); }
            if (conn != null) try { conn.close(); } catch (Exception e) { log.debug("关闭Connection", e); }
        }
    }
}
