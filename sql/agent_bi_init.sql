-- ----------------------------
-- Agent BI V1 MVP 数据库初始化脚本
-- ----------------------------

-- ----------------------------
-- 1、数据源表
-- ----------------------------
drop table if exists bi_datasource;
create table bi_datasource (
  id                bigint(20)      not null auto_increment    comment '数据源ID',
  name              varchar(100)    not null                   comment '数据源名称',
  type              varchar(20)     not null                   comment '数据源类型（DATABASE/API）',
  sub_type          varchar(50)     default null               comment '子类型（MySQL/PostgreSQL/SQLServer/Oracle/REST）',
  host              varchar(255)    default null               comment '主机地址',
  port              int(11)         default null               comment '端口号',
  database_name     varchar(100)    default null               comment '数据库名',
  url               varchar(500)    default null               comment '连接URL（API类型时）',
  username          varchar(100)    default null               comment '用户名（加密存储）',
  password          varchar(500)    default null               comment '密码（加密存储）',
  auth_type         varchar(50)     default null               comment '认证类型（USERNAME_PASSWORD/API_KEY/BASIC_AUTH/OAUTH2）',
  auth_config       text                                         comment '认证配置（JSON格式，加密存储）',
  status            varchar(20)     default 'ACTIVE'            comment '状态（ACTIVE/INACTIVE）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (id)
) engine=innodb auto_increment=1 comment = '数据源表';

-- ----------------------------
-- 2、ETL任务表
-- ----------------------------
drop table if exists bi_etl_task;
create table bi_etl_task (
  id                bigint(20)      not null auto_increment    comment '任务ID',
  name              varchar(100)    not null                   comment '任务名称',
  datasource_id     bigint(20)      not null                   comment '数据源ID',
  source_type       varchar(20)     not null                   comment '源类型（TABLE/SQL/API）',
  source_config     text                                         comment '源配置（表名/SQL/API配置，JSON格式）',
  target_table      varchar(100)    not null                   comment '目标表名',
  extract_mode      varchar(20)     not null                   comment '抽取方式（FULL/INCREMENTAL）',
  increment_field   varchar(100)    default null               comment '增量字段（时间戳/自增ID）',
  increment_type    varchar(20)     default null               comment '增量类型（TIMESTAMP/AUTO_INCREMENT/CDC）',
  schedule_type     varchar(20)     default 'MANUAL'           comment '调度类型（CRON/MANUAL）',
  cron_expression   varchar(100)    default null               comment 'Cron表达式',
  retry_count       int(11)         default 3                  comment '重试次数（默认3）',
  retry_interval    varchar(100)    default '[1,5,15]'          comment '重试间隔（JSON数组：[1,5,15]分钟）',
  status            varchar(20)     default 'ACTIVE'            comment '状态（ACTIVE/INACTIVE/PAUSED）',
  last_run_time     datetime        default null               comment '最后运行时间',
  next_run_time     datetime        default null               comment '下次运行时间',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (id),
  key idx_datasource_id (datasource_id),
  key idx_status (status),
  key idx_next_run_time (next_run_time)
) engine=innodb auto_increment=1 comment = 'ETL任务表';

-- ----------------------------
-- 3、ETL任务执行记录表
-- ----------------------------
drop table if exists bi_etl_task_execution;
create table bi_etl_task_execution (
  id                bigint(20)      not null auto_increment    comment '执行记录ID',
  task_id           bigint(20)      not null                   comment '任务ID',
  status            varchar(20)     not null                   comment '执行状态（RUNNING/SUCCESS/FAILED）',
  start_time        datetime        not null                   comment '开始时间',
  end_time          datetime        default null               comment '结束时间',
  duration          bigint(20)      default null               comment '执行耗时（毫秒）',
  data_count        bigint(20)      default 0                  comment '抽取数据量',
  error_message     text                                         comment '错误信息',
  checkpoint        text                                         comment '断点信息（JSON格式）',
  create_time       datetime                                   comment '创建时间',
  primary key (id),
  key idx_task_id (task_id),
  key idx_status (status),
  key idx_start_time (start_time)
) engine=innodb auto_increment=1 comment = 'ETL任务执行记录表';

-- ----------------------------
-- 4、业务域表
-- ----------------------------
drop table if exists bi_business_domain;
create table bi_business_domain (
  id                bigint(20)      not null auto_increment    comment '业务域ID',
  name              varchar(100)    not null                   comment '业务域名称',
  code              varchar(50)     not null                   comment '业务域编码',
  description       varchar(1000)   default null               comment '业务域描述',
  parent_id         bigint(20)      default 0                  comment '父业务域ID（支持层级）',
  sort_order        int(11)         default 0                  comment '排序',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (id),
  unique key uk_code (code),
  key idx_parent_id (parent_id)
) engine=innodb auto_increment=1 comment = '业务域表';

-- ----------------------------
-- 5、表元数据表
-- ----------------------------
drop table if exists bi_table_metadata;
create table bi_table_metadata (
  id                bigint(20)      not null auto_increment    comment '表元数据ID',
  table_name        varchar(100)    not null                   comment '表名',
  table_comment     varchar(500)    default null               comment '表注释',
  business_description text                                      comment '业务描述',
  domain_id         bigint(20)      not null                   comment '业务域ID',
  vector_id         varchar(100)    default null               comment '向量ID（Qdrant中的ID）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (id),
  unique key uk_table_name (table_name),
  key idx_domain_id (domain_id)
) engine=innodb auto_increment=1 comment = '表元数据表';

-- ----------------------------
-- 6、字段元数据表
-- ----------------------------
drop table if exists bi_field_metadata;
create table bi_field_metadata (
  id                bigint(20)      not null auto_increment    comment '字段元数据ID',
  table_id          bigint(20)      not null                   comment '表元数据ID',
  field_name        varchar(100)    not null                   comment '字段名',
  field_type        varchar(50)     default null               comment '字段类型',
  field_comment     varchar(500)    default null               comment '字段注释',
  business_alias    varchar(100)    default null               comment '业务别名',
  business_description varchar(1000) default null              comment '业务描述',
  enum_values       text                                         comment '枚举值释义（JSON格式）',
  is_sensitive      tinyint(1)      default 0                  comment '是否敏感字段（0否 1是）',
  desensitize_rule  varchar(100)    default null               comment '脱敏规则',
  vector_id         varchar(100)    default null               comment '向量ID（Qdrant中的ID）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (id),
  key idx_table_id (table_id),
  key idx_field_name (field_name),
  unique key uk_table_field (table_id, field_name)
) engine=innodb auto_increment=1 comment = '字段元数据表';

-- ----------------------------
-- 7、原子指标表
-- ----------------------------
drop table if exists bi_atomic_metric;
create table bi_atomic_metric (
  id                bigint(20)      not null auto_increment    comment '指标ID',
  name              varchar(100)    not null                   comment '指标名称',
  code              varchar(50)     not null                   comment '指标编码',
  expression        varchar(500)    not null                   comment '指标表达式（如SUM(amount)）',
  domain_id         bigint(20)      not null                   comment '业务域ID',
  description       varchar(1000)   default null               comment '指标描述',
  vector_id         varchar(100)    default null               comment '向量ID（Qdrant中的ID）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (id),
  unique key uk_code (code),
  key idx_domain_id (domain_id)
) engine=innodb auto_increment=1 comment = '原子指标表';

-- ----------------------------
-- 8、维度表
-- ----------------------------
drop table if exists bi_dimension;
create table bi_dimension (
  id                bigint(20)      not null auto_increment    comment '维度ID',
  name              varchar(100)    not null                   comment '维度名称',
  code              varchar(50)     not null                   comment '维度编码',
  type              varchar(50)     not null                   comment '维度类型（TIME/ORG/PRODUCT/CUSTOM）',
  field_name        varchar(100)    default null               comment '关联字段名',
  domain_id         bigint(20)      not null                   comment '业务域ID',
  description       varchar(1000)   default null               comment '维度描述',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (id),
  unique key uk_code (code),
  key idx_domain_id (domain_id)
) engine=innodb auto_increment=1 comment = '维度表';

-- ----------------------------
-- 9、查询会话表
-- ----------------------------
drop table if exists bi_query_session;
create table bi_query_session (
  id                bigint(20)      not null auto_increment    comment '会话ID',
  user_id           bigint(20)      not null                   comment '用户ID',
  session_key       varchar(100)    not null                   comment '会话键（前端生成）',
  context           text                                         comment '对话上下文（JSON格式）',
  last_active_time  datetime        not null                   comment '最后活跃时间',
  create_time       datetime                                   comment '创建时间',
  expire_time       datetime        not null                   comment '过期时间（30分钟后）',
  primary key (id),
  unique key uk_session_key (session_key),
  key idx_user_id (user_id),
  key idx_expire_time (expire_time)
) engine=innodb auto_increment=1 comment = '查询会话表';

-- ----------------------------
-- 10、查询记录表
-- ----------------------------
drop table if exists bi_query_record;
create table bi_query_record (
  id                bigint(20)      not null auto_increment    comment '查询记录ID',
  session_id        bigint(20)      not null                   comment '会话ID',
  user_id           bigint(20)      not null                   comment '用户ID',
  question          varchar(1000)   not null                   comment '用户问题',
  generated_sql     text                                         comment '生成的SQL',
  executed_sql      text                                         comment '实际执行的SQL（含权限注入）',
  result_count      bigint(20)      default 0                  comment '结果数量',
  execution_time    bigint(20)      default null               comment '执行耗时（毫秒）',
  status            varchar(20)     default 'SUCCESS'          comment '状态（SUCCESS/FAILED/EMPTY）',
  error_message     text                                         comment '错误信息',
  create_time       datetime                                   comment '创建时间',
  primary key (id),
  key idx_session_id (session_id),
  key idx_user_id (user_id),
  key idx_create_time (create_time)
) engine=innodb auto_increment=1 comment = '查询记录表';

-- ----------------------------
-- 11、LLM请求审计表
-- ----------------------------
drop table if exists bi_llm_audit;
create table bi_llm_audit (
  id                bigint(20)      not null auto_increment    comment '审计记录ID',
  query_id          bigint(20)      default null               comment '查询记录ID',
  user_id           bigint(20)      not null                   comment '用户ID',
  original_question text                                         comment '原始问题（PII脱敏后）',
  recalled_tables   text                                         comment '召回的表结构（JSON格式）',
  generated_sql     text                                         comment '生成的SQL',
  model_name        varchar(100)    default null               comment '使用的模型名称',
  token_usage       text                                         comment 'Token使用情况（JSON格式）',
  response_time     bigint(20)      default null               comment '响应时间（毫秒）',
  create_time       datetime                                   comment '创建时间',
  primary key (id),
  key idx_query_id (query_id),
  key idx_user_id (user_id)
) engine=innodb auto_increment=1 comment = 'LLM请求审计表';

-- ----------------------------
-- 11.1、查询反馈表
-- ----------------------------
drop table if exists bi_query_feedback;
create table bi_query_feedback (
  id                bigint(20)      not null auto_increment    comment '反馈ID',
  query_id          bigint(20)      not null                   comment '查询记录ID',
  user_id           bigint(20)      not null                   comment '用户ID',
  feedback_type     varchar(20)     not null                   comment '反馈类型（CORRECT/INCORRECT/SUGGESTION）',
  content           text                                         comment '反馈内容',
  suggested_sql     text                                         comment '建议的SQL（INCORRECT时）',
  review_status     varchar(20)     default 'PENDING'           comment '审核状态（PENDING/APPROVED/REJECTED）',
  review_comment    text                                         comment '审核意见',
  reviewer          varchar(64)     default null               comment '审核人',
  review_time       datetime        default null               comment '审核时间',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default null               comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (id),
  key idx_query_id (query_id),
  key idx_user_id (user_id),
  key idx_review_status (review_status)
) engine=innodb auto_increment=1 comment = '查询反馈表';

-- ----------------------------
-- 12、图表卡片表
-- ----------------------------
drop table if exists bi_chart_card;
create table bi_chart_card (
  id                bigint(20)      not null auto_increment    comment '卡片ID',
  name              varchar(100)    not null                   comment '卡片名称',
  user_id           bigint(20)      not null                   comment '用户ID',
  query_id          bigint(20)      default null               comment '关联的查询记录ID',
  chart_type        varchar(50)     not null                   comment '图表类型',
  chart_config      text                                         comment '图表配置（ECharts option，JSON格式）',
  `sql`             text                                         comment '关联的SQL',
  permission_tags   text                                         comment '权限标签（JSON格式）',
  last_refresh_time datetime        default null               comment '最后刷新时间',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (id),
  key idx_user_id (user_id),
  key idx_create_time (create_time)
) engine=innodb auto_increment=1 comment = '图表卡片表';

-- ----------------------------
-- 13、看板表
-- ----------------------------
drop table if exists bi_dashboard;
create table bi_dashboard (
  id                bigint(20)      not null auto_increment    comment '看板ID',
  name              varchar(100)    not null                   comment '看板名称',
  description       varchar(500)    default null               comment '看板描述',
  cover             varchar(500)    default null               comment '封面图片URL',
  user_id           bigint(20)      not null                   comment '用户ID',
  layout            text                                         comment '布局配置（JSON格式，包含卡片位置和大小）',
  refresh_interval  int(11)        default 0                    comment '刷新间隔（分钟）',
  is_public         tinyint(1)     default 0                    comment '是否公开',
  refresh_frequency varchar(20)     default 'MANUAL'            comment '刷新频率（MINUTE/HOUR/DAY/MANUAL，预留）',
  refresh_cron      varchar(100)    default null               comment '刷新Cron表达式（预留）',
  status            varchar(20)     default 'ACTIVE'           comment '状态（ACTIVE/INACTIVE）',
  background_config text             default null               comment '背景配置JSON',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (id),
  key idx_user_id (user_id),
  key idx_status (status)
) engine=innodb auto_increment=1 comment = '看板表';

-- ----------------------------
-- 14、看板卡片关联表
-- ----------------------------
drop table if exists bi_dashboard_card;
create table bi_dashboard_card (
  id                bigint(20)      not null auto_increment    comment '关联ID',
  dashboard_id      bigint(20)      not null                   comment '看板ID',
  card_id           bigint(20)      default null               comment '图表卡片ID',
  position_x        int(11)         default 0                  comment 'X坐标',
  position_y        int(11)         default 0                  comment 'Y坐标',
  width             int(11)         default 200                comment '宽度',
  height            int(11)         default 150                comment '高度',
  sort_order        int(11)         default 0                  comment '排序',
  component_type    varchar(20)     not null default 'chart'   comment '组件类型: chart/decoration/group/datasource',
  style_config      text             default null               comment '样式配置JSON',
  parent_id         bigint(20)      default null               comment '父组合ID',
  decoration_type   varchar(50)     default null               comment '装饰组件子类型',
  card_name         varchar(200)    default null               comment '组件显示名称',
  create_time       datetime                                   comment '创建时间',
  primary key (id),
  key idx_dashboard_id (dashboard_id),
  key idx_card_id (card_id),
  key idx_parent_id (parent_id),
  key idx_component_type (component_type),
  unique key uk_dashboard_card (dashboard_id, card_id)
) engine=innodb auto_increment=1 comment = '看板卡片关联表';

-- ----------------------------
-- 14.1、数据源卡片配置表
-- ----------------------------
drop table if exists bi_datasource_card_config;
create table bi_datasource_card_config (
  id                 bigint(20)     not null auto_increment   comment '配置ID',
  dashboard_card_id  bigint(20)     not null                   comment '关联看板卡片ID',
  datasource_id      bigint(20)     not null                   comment '数据源ID',
  query_type         varchar(10)   not null                   comment '查询类型: SQL/API',
  sql_template       text           default null               comment 'SQL查询模板',
  api_url            varchar(500)   default null               comment 'API地址',
  api_method         varchar(10)   default 'GET'              comment 'HTTP方法',
  api_headers        text           default null               comment '自定义Header JSON',
  api_body           text           default null               comment '请求体',
  response_data_path varchar(200)  default null               comment '响应数据提取路径',
  chart_type         varchar(20)   not null                   comment '图表类型',
  chart_config_override text       default null               comment '图表配置覆盖',
  column_mapping     text           default null               comment '列映射配置JSON',
  refresh_interval   int(11)        default 0                  comment '独立刷新间隔（分钟）',
  query_timeout      int(11)        default 30                 comment '查询超时（秒）',
  max_rows           int(11)        default 10000               comment '最大返回行数',
  create_by          varchar(64)    default ''                 comment '创建者',
  create_time        datetime       default null               comment '创建时间',
  update_by          varchar(64)    default ''                 comment '更新者',
  update_time        datetime       default null               comment '更新时间',
  primary key (id),
  unique key uk_dashboard_card_id (dashboard_card_id),
  key idx_datasource_id (datasource_id)
) engine=innodb auto_increment=1 comment = '数据源卡片查询配置';

-- ----------------------------
-- 15、分享链接表
-- ----------------------------
drop table if exists bi_share_link;
create table bi_share_link (
  id                bigint(20)      not null auto_increment    comment '分享链接ID',
  share_key         varchar(100)    not null                   comment '分享键（唯一标识）',
  resource_type     varchar(20)     not null                   comment '资源类型（DASHBOARD/CARD）',
  resource_id       bigint(20)      not null                   comment '资源ID',
  user_id           bigint(20)      not null                   comment '创建者ID',
  password          varchar(500)    default null               comment '访问密码（加密存储）',
  expire_time       datetime        not null                   comment '过期时间',
  access_count      bigint(20)      default 0                  comment '访问次数',
  status            varchar(20)     default 'ACTIVE'           comment '状态（ACTIVE/EXPIRED/DISABLED）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (id),
  unique key uk_share_key (share_key),
  key idx_status (status),
  key idx_expire_time (expire_time)
) engine=innodb auto_increment=1 comment = '分享链接表';

-- ----------------------------
-- 16、数据权限配置表
-- ----------------------------
drop table if exists bi_data_permission;
create table bi_data_permission (
  id                bigint(20)      not null auto_increment    comment '权限配置ID',
  user_id           bigint(20)      default null               comment '用户ID（可为空，表示角色权限）',
  role_id           bigint(20)      default null               comment '角色ID（可为空，表示用户权限）',
  resource_type     varchar(20)     not null                   comment '资源类型（TABLE/FIELD）',
  resource_id       bigint(20)      not null                   comment '资源ID（表ID或字段ID）',
  permission_type   varchar(20)     not null                   comment '权限类型（ALLOW/DENY）',
  row_filter        text                                         comment '行级过滤条件（JSON格式）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (id),
  key idx_user_id (user_id),
  key idx_role_id (role_id),
  key idx_resource (resource_type, resource_id)
) engine=innodb auto_increment=1 comment = '数据权限配置表';

-- ----------------------------
-- 17、看板菜单初始化（编辑页、查看页隐藏菜单）
-- ----------------------------
-- 依赖「看板管理」菜单已存在（component='dashboard/index'），编辑页、查看页作为其同级菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '编辑看板', parent_id, 100, 'dashboard/edit', 'dashboard/edit', NULL, '', 1, 0, 'C', '1', '0', 'dashboard:edit', '#', 'admin', sysdate(), '编辑看板（隐藏）'
FROM sys_menu
WHERE component = 'dashboard/index'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE component = 'dashboard/edit')
LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '查看看板', parent_id, 101, 'dashboard/view', 'dashboard/view', NULL, '', 1, 0, 'C', '1', '0', 'dashboard:list', '#', 'admin', sysdate(), '查看看板（隐藏）'
FROM sys_menu
WHERE component = 'dashboard/index'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE component = 'dashboard/view')
LIMIT 1;
