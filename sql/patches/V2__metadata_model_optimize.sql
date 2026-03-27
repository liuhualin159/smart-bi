-- ----------------------------
-- 元数据精简版数据模型（001-metadata-model-optimize）
-- 执行顺序：在 agent_bi_init.sql 或现有库上执行；若依 sys_dict_* / sys_config 需已存在
-- ----------------------------

-- 1) bi_table_metadata 扩展
ALTER TABLE bi_table_metadata
  ADD COLUMN table_usage varchar(30) DEFAULT 'PRIMARY' COMMENT '用途：PRIMARY/DIM/AGG/AUX/TEST/HIST',
  ADD COLUMN nl2sql_visibility_level varchar(20) DEFAULT 'NORMAL' COMMENT 'NL2SQL可见性：HIDDEN/NORMAL/PREFERRED',
  ADD COLUMN grain_desc varchar(200) DEFAULT NULL COMMENT '业务粒度描述';

-- 2) bi_field_metadata 扩展
ALTER TABLE bi_field_metadata
  ADD COLUMN usage_type varchar(30) DEFAULT NULL COMMENT '用途：DIMENSION/MEASURE/OTHER',
  ADD COLUMN semantic_type varchar(30) DEFAULT NULL COMMENT '语义类型：ID/NAME/CODE/TIME/AMOUNT/COUNT/RATIO/FLAG',
  ADD COLUMN unit varchar(20) DEFAULT NULL COMMENT '单位：元/次/人/%',
  ADD COLUMN default_agg_func varchar(30) DEFAULT NULL COMMENT '默认聚合：SUM/COUNT/COUNT_DISTINCT/AVG/MAX/MIN',
  ADD COLUMN allowed_agg_funcs varchar(200) DEFAULT NULL COMMENT '允许聚合列表JSON或逗号分隔',
  ADD COLUMN nl2sql_priority int(11) DEFAULT NULL COMMENT 'NL2SQL优先级1-10',
  ADD COLUMN sensitive_level varchar(20) DEFAULT NULL COMMENT '敏感级别：LOW/MEDIUM/HIGH',
  ADD COLUMN exposure_policy varchar(20) DEFAULT NULL COMMENT '曝光策略：MASK/AGG_ONLY/FORBIDDEN';

-- 3) bi_field_alias 新表
CREATE TABLE IF NOT EXISTS bi_field_alias (
  id          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  field_id    bigint(20)   NOT NULL COMMENT '字段元数据ID',
  alias       varchar(200) NOT NULL COMMENT '别名内容',
  source      varchar(30)  DEFAULT 'HUMAN' COMMENT '来源：HUMAN/AUTO_SUGGEST/INFERRED_FROM_SQL',
  create_by   varchar(64)  DEFAULT '' COMMENT '创建者',
  create_time datetime    DEFAULT NULL COMMENT '创建时间',
  update_by   varchar(64)  DEFAULT '' COMMENT '更新者',
  update_time datetime    DEFAULT NULL COMMENT '更新时间',
  remark      varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_field_alias (field_id, alias),
  KEY idx_field_id (field_id),
  KEY idx_alias (alias)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='字段别名表';

-- 4) bi_atomic_metric 扩展
ALTER TABLE bi_atomic_metric
  ADD COLUMN metric_grain varchar(200) DEFAULT NULL COMMENT '统计粒度说明',
  ADD COLUMN metric_filter varchar(500) DEFAULT NULL COMMENT '口径过滤前提(SQL或自然语言)';

-- 5) bi_table_relation 新表
CREATE TABLE IF NOT EXISTS bi_table_relation (
  id            bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  left_table    varchar(100) NOT NULL COMMENT '左表名',
  left_field    varchar(100) NOT NULL COMMENT '左表字段',
  right_table   varchar(100) NOT NULL COMMENT '右表名',
  right_field   varchar(100) NOT NULL COMMENT '右表字段',
  relation_type varchar(30)  DEFAULT NULL COMMENT '关系类型',
  priority      int(11)      DEFAULT 0 COMMENT '优先级',
  create_by     varchar(64)  DEFAULT '' COMMENT '创建者',
  create_time   datetime    DEFAULT NULL COMMENT '创建时间',
  update_by     varchar(64)  DEFAULT '' COMMENT '更新者',
  update_time   datetime    DEFAULT NULL COMMENT '更新时间',
  remark        varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_left_table (left_table),
  KEY idx_right_table (right_table)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='表推荐关系(推荐join)';

-- 6) bi_llm_audit 扩展
ALTER TABLE bi_llm_audit
  ADD COLUMN prompt_version varchar(50) DEFAULT NULL COMMENT '提示词版本',
  ADD COLUMN meta_schema_version varchar(50) DEFAULT NULL COMMENT '元数据schema版本',
  ADD COLUMN error_category varchar(30) DEFAULT NULL COMMENT '错误类型：WRONG_TABLE/WRONG_FIELD/WRONG_JOIN/WRONG_CONDITION/PERFORMANCE/PERMISSION_DENIED/OTHER',
  ADD COLUMN process_status varchar(20) DEFAULT '未处理' COMMENT '歧义优化状态：未处理/已处理';
