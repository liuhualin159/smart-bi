-- bi_share_link 表补充审计列：create_by、update_by、remark
-- 若已执行过 agent_bi_init.sql，表结构缺少这些列会导致 ShareLinkMapper 插入报错

ALTER TABLE bi_share_link
  ADD COLUMN create_by varchar(64) DEFAULT '' COMMENT '创建者' AFTER status,
  ADD COLUMN update_by varchar(64) DEFAULT '' COMMENT '更新者' AFTER create_time,
  ADD COLUMN remark varchar(500) DEFAULT NULL COMMENT '备注' AFTER update_time;
