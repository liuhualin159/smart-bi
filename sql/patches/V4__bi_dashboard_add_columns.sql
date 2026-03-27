-- bi_dashboard 表补充应用所需列：refresh_interval（分钟）、is_public
-- 若已执行过 agent_bi_init.sql 旧版本，请执行本脚本

ALTER TABLE bi_dashboard
  ADD COLUMN refresh_interval int(11) DEFAULT 0 COMMENT '刷新间隔（分钟）',
  ADD COLUMN is_public tinyint(1) DEFAULT 0 COMMENT '是否公开';
