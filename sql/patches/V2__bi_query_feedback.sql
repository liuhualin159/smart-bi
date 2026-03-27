-- 创建查询反馈表（若 agent_bi_init.sql 未包含此表，请执行本脚本）
CREATE TABLE IF NOT EXISTS bi_query_feedback (
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
