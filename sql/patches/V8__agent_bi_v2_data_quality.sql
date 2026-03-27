-- ----------------------------
-- Agent BI V2 数据质量监控（003-agent-bi-v2 Phase 5）
-- 依赖：bi_table_metadata 已存在
-- ----------------------------

-- 1) bi_quality_rule（质量规则）
CREATE TABLE IF NOT EXISTS bi_quality_rule (
  id                bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  table_id          bigint(20)   NOT NULL COMMENT '关联表ID(bi_table_metadata.id)',
  rule_type         varchar(50)  NOT NULL COMMENT '规则类型：COMPLETENESS/ACCURACY/CONSISTENCY/UNIQUENESS/TIMELINESS',
  rule_config       text         NOT NULL COMMENT 'JSON配置(字段、阈值等)',
  priority          int          DEFAULT 0 COMMENT '优先级',
  severity_weight   int          DEFAULT 1 COMMENT '严重性权重(用于评分)',
  status            char(1)      DEFAULT '0' COMMENT '状态(0正常 1停用)',
  create_by         varchar(64)  DEFAULT '' COMMENT '创建者',
  create_time       datetime     DEFAULT NULL COMMENT '创建时间',
  update_by         varchar(64)  DEFAULT '' COMMENT '更新者',
  update_time       datetime     DEFAULT NULL COMMENT '更新时间',
  remark            varchar(500)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_table_id (table_id),
  KEY idx_rule_type (rule_type),
  KEY idx_status (status)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='数据质量规则表';

-- 2) bi_quality_score（质量评分）
CREATE TABLE IF NOT EXISTS bi_quality_score (
  id                bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  table_id          bigint(20)   NOT NULL COMMENT '关联表ID',
  score             int          NOT NULL COMMENT '评分0-100',
  score_type        varchar(20)  NOT NULL COMMENT 'TABLE/FIELD',
  field_id          bigint(20)   DEFAULT NULL COMMENT '字段ID(score_type=FIELD时)',
  calculated_at      datetime     NOT NULL COMMENT '计算时间',
  create_by         varchar(64)  DEFAULT '' COMMENT '创建者',
  create_time       datetime     DEFAULT NULL COMMENT '创建时间',
  remark            varchar(500)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_table_id (table_id),
  KEY idx_calculated_at (calculated_at),
  KEY idx_score_type (score_type)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='数据质量评分表';

-- 3) bi_subscription（订阅，bi_push_record 依赖）
CREATE TABLE IF NOT EXISTS bi_subscription (
  id                bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id           bigint(20)   NOT NULL COMMENT '订阅用户ID',
  subscribe_type    varchar(50)  NOT NULL COMMENT 'DASHBOARD/CARD/QUERY/QUALITY_REPORT',
  target_id         bigint(20)   DEFAULT NULL COMMENT '看板/卡片/查询ID',
  schedule_cron    varchar(100)  NOT NULL COMMENT 'Cron表达式',
  receive_channels  varchar(200) NOT NULL COMMENT '邮件/钉钉/企业微信/站内/短信',
  status            varchar(20)  NOT NULL COMMENT 'ENABLED/DISABLED',
  create_by         varchar(64)  DEFAULT '' COMMENT '创建者',
  create_time       datetime     DEFAULT NULL COMMENT '创建时间',
  update_by         varchar(64)  DEFAULT '' COMMENT '更新者',
  update_time       datetime     DEFAULT NULL COMMENT '更新时间',
  remark            varchar(500)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_status (status)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='报表订阅表';

-- 4) bi_push_record（推送记录）
CREATE TABLE IF NOT EXISTS bi_push_record (
  id                bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  subscription_id   bigint(20)   DEFAULT NULL COMMENT '关联订阅ID',
  push_at           datetime     NOT NULL COMMENT '推送时间',
  status            varchar(20)  NOT NULL COMMENT 'SUCCESS/FAILED',
  retry_count       int          DEFAULT 0 COMMENT '重试次数',
  create_time       datetime     DEFAULT NULL COMMENT '创建时间',
  remark            varchar(500)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_subscription_id (subscription_id),
  KEY idx_push_at (push_at),
  KEY idx_status (status)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='推送记录表';

-- 5) 数据质量菜单与权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据质量', 0, 12, 'quality', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'validCode', 'admin', sysdate(), '数据质量监控'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '数据质量' AND parent_id = 0);

SET @quality_parent = (SELECT menu_id FROM sys_menu WHERE menu_name = '数据质量' AND parent_id = 0 LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '质量规则', @quality_parent, 1, 'rule', 'quality/index', NULL, '', 1, 0, 'C', '0', '0', 'bi:quality:list', 'list', 'admin', sysdate(), '质量规则与报告'
FROM (SELECT 1) AS t WHERE @quality_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'bi:quality:list');

SET @rule_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'bi:quality:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '规则查询', @rule_menu, 1, '', '', NULL, '', 1, 0, 'F', '0', '0', 'bi:quality:query', '#', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE @rule_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'bi:quality:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '规则新增', @rule_menu, 2, '', '', NULL, '', 1, 0, 'F', '0', '0', 'bi:quality:add', '#', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE @rule_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'bi:quality:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '规则修改', @rule_menu, 3, '', '', NULL, '', 1, 0, 'F', '0', '0', 'bi:quality:edit', '#', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE @rule_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'bi:quality:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '规则删除', @rule_menu, 4, '', '', NULL, '', 1, 0, 'F', '0', '0', 'bi:quality:remove', '#', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE @rule_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'bi:quality:remove');
