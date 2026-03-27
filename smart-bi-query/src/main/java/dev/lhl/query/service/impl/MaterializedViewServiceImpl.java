package dev.lhl.query.service.impl;

import dev.lhl.query.service.IMaterializedViewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 物化视图服务实现（占位）
 * MySQL 无原生物化视图，后续可扩展：bi_materialized_view 表存定义，刷新时执行 CREATE TABLE AS / REPLACE INTO
 *
 * @author smart-bi
 */
@Service
public class MaterializedViewServiceImpl implements IMaterializedViewService {

    private static final Logger log = LoggerFactory.getLogger(MaterializedViewServiceImpl.class);

    @Override
    public List<Map<String, Object>> listMaterializedViews() {
        log.debug("物化视图列表（占位）");
        return Collections.emptyList();
    }

    @Override
    public boolean refresh(String viewName) {
        log.info("物化视图刷新（占位）: viewName={}", viewName);
        return false;
    }
}
