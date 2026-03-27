-- 修复 bi_llm_audit 表：query_id 允许为空（审计记录插入时查询记录可能尚未生成）
-- 若已执行过 agent_bi_init.sql 旧版本，请执行本脚本
ALTER TABLE bi_llm_audit MODIFY COLUMN query_id bigint(20) DEFAULT NULL COMMENT '查询记录ID';
