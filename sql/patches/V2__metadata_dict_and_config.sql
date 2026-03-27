-- ----------------------------
-- 元数据管理：字典与系统参数（001-metadata-model-optimize）
-- 依赖：若依 sys_dict_type、sys_dict_data、sys_config 已存在
-- ----------------------------

-- 字典类型（使用较大 dict_id 避免与现有冲突）
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
SELECT 110, '表用途', 'bi_table_usage', '0', 'admin', sysdate(), '表元数据用途'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'bi_table_usage');

INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
SELECT 111, 'NL2SQL可见性', 'bi_nl2sql_visibility', '0', 'admin', sysdate(), '表对NL2SQL的可见性'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'bi_nl2sql_visibility');

INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
SELECT 112, 'NL2SQL错误类型', 'bi_error_category', '0', 'admin', sysdate(), '审计错误分类'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'bi_error_category');

INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
SELECT 113, '字段用途', 'bi_field_usage_type', '0', 'admin', sysdate(), '字段用途维度/指标/其他'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'bi_field_usage_type');

INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
SELECT 114, '曝光策略', 'bi_exposure_policy', '0', 'admin', sysdate(), '敏感字段曝光策略'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'bi_exposure_policy');

-- 表用途 bi_table_usage
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '主事实表', 'PRIMARY', 'bi_table_usage', 'primary', 'Y', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_table_usage' AND dict_value = 'PRIMARY');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '维度表', 'DIM', 'bi_table_usage', '', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_table_usage' AND dict_value = 'DIM');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '聚合/宽表', 'AGG', 'bi_table_usage', '', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_table_usage' AND dict_value = 'AGG');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '辅助表', 'AUX', 'bi_table_usage', 'info', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_table_usage' AND dict_value = 'AUX');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '测试表', 'TEST', 'bi_table_usage', 'warning', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_table_usage' AND dict_value = 'TEST');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '历史/归档表', 'HIST', 'bi_table_usage', 'info', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_table_usage' AND dict_value = 'HIST');

-- NL2SQL可见性 bi_nl2sql_visibility
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '隐藏', 'HIDDEN', 'bi_nl2sql_visibility', 'danger', 'N', '0', 'admin', sysdate(), '对NL2SQL不可见'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_nl2sql_visibility' AND dict_value = 'HIDDEN');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '普通', 'NORMAL', 'bi_nl2sql_visibility', 'primary', 'Y', '0', 'admin', sysdate(), '普通候选表'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_nl2sql_visibility' AND dict_value = 'NORMAL');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '优先', 'PREFERRED', 'bi_nl2sql_visibility', 'success', 'N', '0', 'admin', sysdate(), '优先候选'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_nl2sql_visibility' AND dict_value = 'PREFERRED');

-- 错误类型 bi_error_category
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '错误表', 'WRONG_TABLE', 'bi_error_category', 'danger', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_error_category' AND dict_value = 'WRONG_TABLE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '错误字段', 'WRONG_FIELD', 'bi_error_category', 'danger', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_error_category' AND dict_value = 'WRONG_FIELD');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '错误JOIN', 'WRONG_JOIN', 'bi_error_category', 'danger', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_error_category' AND dict_value = 'WRONG_JOIN');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '错误条件', 'WRONG_CONDITION', 'bi_error_category', 'warning', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_error_category' AND dict_value = 'WRONG_CONDITION');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '性能', 'PERFORMANCE', 'bi_error_category', 'info', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_error_category' AND dict_value = 'PERFORMANCE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '权限拒绝', 'PERMISSION_DENIED', 'bi_error_category', 'danger', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_error_category' AND dict_value = 'PERMISSION_DENIED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 7, '其他', 'OTHER', 'bi_error_category', 'info', 'Y', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_error_category' AND dict_value = 'OTHER');

-- 字段用途 bi_field_usage_type
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '维度', 'DIMENSION', 'bi_field_usage_type', 'primary', 'Y', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_field_usage_type' AND dict_value = 'DIMENSION');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '指标', 'MEASURE', 'bi_field_usage_type', 'success', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_field_usage_type' AND dict_value = 'MEASURE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '其他', 'OTHER', 'bi_field_usage_type', 'info', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_field_usage_type' AND dict_value = 'OTHER');

-- 曝光策略 bi_exposure_policy
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '脱敏展示', 'MASK', 'bi_exposure_policy', 'warning', 'Y', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_exposure_policy' AND dict_value = 'MASK');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '仅聚合', 'AGG_ONLY', 'bi_exposure_policy', 'info', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_exposure_policy' AND dict_value = 'AGG_ONLY');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '禁止SELECT', 'FORBIDDEN', 'bi_exposure_policy', 'danger', 'N', '0', 'admin', sysdate(), '仅WHERE/JOIN'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'bi_exposure_policy' AND dict_value = 'FORBIDDEN');

-- 系统参数：问题表高亮（若依 sys_config）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 'NL2SQL问题表统计时间窗口(天)', 'nl2sql.problemTable.windowDays', '30', 'N', 'admin', sysdate(), '', null, '问题表错误次数统计时间范围，默认30天'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'nl2sql.problemTable.windowDays');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 'NL2SQL问题表错误次数阈值', 'nl2sql.problemTable.errorCountThreshold', '3', 'N', 'admin', sysdate(), '', null, '达到该次数即高亮为问题表，默认3'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'nl2sql.problemTable.errorCountThreshold');
