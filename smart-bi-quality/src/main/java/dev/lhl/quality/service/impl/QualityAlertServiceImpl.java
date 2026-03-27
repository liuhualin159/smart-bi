package dev.lhl.quality.service.impl;

import dev.lhl.metadata.domain.TableMetadata;
import dev.lhl.metadata.service.IMetadataService;
import dev.lhl.quality.domain.BiQualityScore;
import dev.lhl.quality.mapper.BiQualityScoreMapper;
import dev.lhl.quality.service.IQualityAlertService;
import dev.lhl.quality.service.IQualityScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 质量告警：评分低于阈值时记录并通知（多通道需外部配置）
 *
 * @author smart-bi
 */
@Service
public class QualityAlertServiceImpl implements IQualityAlertService {

    private static final Logger log = LoggerFactory.getLogger(QualityAlertServiceImpl.class);

    @Autowired(required = false)
    private IMetadataService metadataService;

    @Autowired
    private BiQualityScoreMapper biQualityScoreMapper;

    @Autowired
    private IQualityScoreService qualityScoreService;

    @Override
    public int checkAndAlert(Long tableId, Integer scoreThreshold) {
        int sent = 0;
        if (scoreThreshold == null) scoreThreshold = 60;
        if (metadataService == null) return sent;

        List<Long> tableIds = new ArrayList<>();
        if (tableId != null) {
            tableIds.add(tableId);
        } else {
            List<TableMetadata> tables = metadataService.selectTableMetadataList(new TableMetadata());
            tables.forEach(t -> tableIds.add(t.getId()));
        }

        for (Long tid : tableIds) {
            BiQualityScore latest = biQualityScoreMapper.selectLatestByTableAndType(tid, "TABLE");
            if (latest == null) continue;
            if (latest.getScore() != null && latest.getScore() < scoreThreshold) {
                TableMetadata t = metadataService.selectTableMetadataById(tid);
                String tableName = t != null ? t.getTableName() : "表" + tid;
                log.warn("质量告警: 表 {} 评分 {} 低于阈值 {}", tableName, latest.getScore(), scoreThreshold);
                // 占位：调用邮件/钉钉/企业微信等，需外部配置
                sent++;
            }
        }
        return sent;
    }
}
