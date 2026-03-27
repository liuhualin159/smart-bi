-- ----------------------------
-- Agent BI V2 报表订阅菜单与权限
-- 依赖：bi_subscription、bi_push_record 已在 V8
-- ----------------------------

-- 订阅推送调度任务（每分钟扫描到期订阅并执行推送）
INSERT INTO sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT 900001, '报表订阅推送', 'SUBSCRIPTION', 'subscriptionPushRunner.execute', '0 * * * * ?', '3', '1', '0', 'admin', sysdate(), '每分钟扫描到期订阅并执行推送'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE job_group = 'SUBSCRIPTION' AND job_name = '报表订阅推送');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报表订阅', 0, 13, 'subscription', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'message', 'admin', sysdate(), '报表订阅与推送'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '报表订阅' AND parent_id = 0);

SET @sub_parent = (SELECT menu_id FROM sys_menu WHERE menu_name = '报表订阅' AND parent_id = 0 LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '订阅管理', @sub_parent, 1, 'index', 'subscription/index', NULL, '', 1, 0, 'C', '0', '0', 'bi:subscription:list', 'list', 'admin', sysdate(), '订阅配置与推送历史'
FROM (SELECT 1) AS t WHERE @sub_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'bi:subscription:list');

SET @sub_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'bi:subscription:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '订阅查询', @sub_menu, 1, '', '', NULL, '', 1, 0, 'F', '0', '0', 'bi:subscription:query', '#', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE @sub_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'bi:subscription:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '订阅新增', @sub_menu, 2, '', '', NULL, '', 1, 0, 'F', '0', '0', 'bi:subscription:add', '#', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE @sub_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'bi:subscription:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '订阅修改', @sub_menu, 3, '', '', NULL, '', 1, 0, 'F', '0', '0', 'bi:subscription:edit', '#', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE @sub_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'bi:subscription:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '订阅删除', @sub_menu, 4, '', '', NULL, '', 1, 0, 'F', '0', '0', 'bi:subscription:remove', '#', 'admin', sysdate(), ''
FROM (SELECT 1) AS t WHERE @sub_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'bi:subscription:remove');
