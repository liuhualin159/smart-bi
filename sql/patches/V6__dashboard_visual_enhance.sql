-- 看板可视化增强：背景配置、组件类型扩展、数据源卡片配置

-- 1. bi_dashboard 新增背景配置字段
ALTER TABLE bi_dashboard
  ADD COLUMN background_config TEXT DEFAULT NULL COMMENT '背景配置JSON';

-- 2. bi_dashboard_card 扩展字段
ALTER TABLE bi_dashboard_card
  ADD COLUMN component_type VARCHAR(20) NOT NULL DEFAULT 'chart' COMMENT '组件类型: chart/decoration/group/datasource',
  ADD COLUMN style_config TEXT DEFAULT NULL COMMENT '样式配置JSON',
  ADD COLUMN parent_id BIGINT DEFAULT NULL COMMENT '父组合ID',
  ADD COLUMN decoration_type VARCHAR(50) DEFAULT NULL COMMENT '装饰组件子类型',
  ADD COLUMN card_name VARCHAR(200) DEFAULT NULL COMMENT '组件显示名称',
  ADD INDEX idx_parent_id (parent_id),
  ADD INDEX idx_component_type (component_type);

ALTER TABLE bi_dashboard_card MODIFY COLUMN card_id BIGINT DEFAULT NULL COMMENT '图表卡片ID';

-- 3. 新建数据源卡片配置表
CREATE TABLE bi_datasource_card_config (
  id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  dashboard_card_id  BIGINT       NOT NULL COMMENT '关联看板卡片ID',
  datasource_id      BIGINT       NOT NULL COMMENT '数据源ID',
  query_type         VARCHAR(10)  NOT NULL COMMENT '查询类型: SQL/API',
  sql_template       TEXT         DEFAULT NULL COMMENT 'SQL查询模板',
  api_url            VARCHAR(500) DEFAULT NULL COMMENT 'API地址',
  api_method         VARCHAR(10)  DEFAULT 'GET' COMMENT 'HTTP方法',
  api_headers        TEXT         DEFAULT NULL COMMENT '自定义Header JSON',
  api_body           TEXT         DEFAULT NULL COMMENT '请求体',
  response_data_path VARCHAR(200) DEFAULT NULL COMMENT '响应数据提取路径',
  chart_type         VARCHAR(20)  NOT NULL COMMENT '图表类型',
  chart_config_override TEXT      DEFAULT NULL COMMENT '图表配置覆盖',
  column_mapping     TEXT         DEFAULT NULL COMMENT '列映射配置JSON',
  refresh_interval   INT          DEFAULT 0 COMMENT '独立刷新间隔（分钟）',
  query_timeout      INT          DEFAULT 30 COMMENT '查询超时（秒）',
  max_rows           INT          DEFAULT 10000 COMMENT '最大返回行数',
  create_by          VARCHAR(64)  DEFAULT '' COMMENT '创建者',
  create_time        DATETIME     DEFAULT NULL COMMENT '创建时间',
  update_by          VARCHAR(64)  DEFAULT '' COMMENT '更新者',
  update_time        DATETIME     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dashboard_card_id (dashboard_card_id),
  KEY idx_datasource_id (datasource_id)
) ENGINE=InnoDB COMMENT='数据源卡片查询配置';
