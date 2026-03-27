-- ----------------------------
-- Agent BI V2 智能问数增强（003-agent-bi-v2）
-- 依赖：若依 sys_config、bi_atomic_metric、bi_field_metadata 已存在
-- ----------------------------

-- 1) bi_atomic_metric 扩展：同比/环比 SQL 模板
ALTER TABLE bi_atomic_metric
  ADD COLUMN temporal_expression varchar(500) DEFAULT NULL COMMENT '同比/环比SQL模板或说明';

-- 2) bi_field_metadata 扩展：下钻路径、显示格式
ALTER TABLE bi_field_metadata
  ADD COLUMN drill_path text DEFAULT NULL COMMENT '下钻路径JSON，如[{"level":"region","field":"region_name"}]',
  ADD COLUMN display_format text DEFAULT NULL COMMENT '显示格式JSON，如{"type":"percent","decimal":2}';

-- 3) bi_feedback_correction 新表
CREATE TABLE IF NOT EXISTS bi_feedback_correction (
  id                bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  query_id          bigint(20)   DEFAULT NULL COMMENT '关联查询记录ID',
  original_question text         NOT NULL COMMENT '原始问题',
  corrected_sql     text         NOT NULL COMMENT '审核通过的正确SQL',
  reviewed_by       bigint(20)   DEFAULT NULL COMMENT '审核人(数据管理员)',
  reviewed_at       datetime     NOT NULL COMMENT '审核时间',
  status            varchar(20)  NOT NULL COMMENT '状态：APPROVED/REJECTED',
  used_in_nl2sql    tinyint(1)   DEFAULT 0 COMMENT '是否已用于NL2SQL注入',
  create_by         varchar(64)  DEFAULT '' COMMENT '创建者',
  create_time       datetime     DEFAULT NULL COMMENT '创建时间',
  update_by         varchar(64)  DEFAULT '' COMMENT '更新者',
  update_time       datetime     DEFAULT NULL COMMENT '更新时间',
  remark            varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_original_question (original_question(100)),
  KEY idx_status (status),
  KEY idx_reviewed_at (reviewed_at)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='反馈修正表(审核通过后用于NL2SQL学习)';

-- 4) bi_query_record 扩展：置信度、澄清问题（若尚未存在）
ALTER TABLE bi_query_record ADD COLUMN confidence double DEFAULT NULL COMMENT 'NL2SQL置信度0-1';
ALTER TABLE bi_query_record ADD COLUMN disambiguation_questions text DEFAULT NULL COMMENT '澄清问题列表JSON';
ALTER TABLE bi_query_record ADD COLUMN involved_tables text DEFAULT NULL COMMENT '涉及表名列表JSON';

-- 5) 系统参数
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 'NL2SQL置信度阈值', 'smart.bi.nl2sql.confidence.threshold', '0.6', 'N', 'admin', sysdate(), '', null, '低于此值反问确认，不执行SQL'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'smart.bi.nl2sql.confidence.threshold');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '反馈注入相似度阈值', 'smart.bi.feedback.similarity.threshold', '0.8', 'N', 'admin', sysdate(), '', null, '相似问题达到此阈值时注入corrected_sql'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'smart.bi.feedback.similarity.threshold');

-- 6) 反馈审核权限菜单（需将权限分配给“数据管理员”角色）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '反馈审核', 2004, 10, '', '', NULL, '', 1, 0, 'F', '0', '0', 'bi:feedback:approve', '#', 'admin', sysdate(), '数据管理员审核反馈的正确SQL'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'bi:feedback:approve');
