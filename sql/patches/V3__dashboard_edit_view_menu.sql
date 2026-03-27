-- 看板管理：新增编辑页、查看页的隐藏菜单（路由由数据库配置，非前端写死）
-- 执行前请确保「看板管理」菜单已存在（component='dashboard/index'）
-- 编辑页、查看页将作为看板管理的同级菜单，path 为 dashboard/edit、dashboard/view，形成 /父path/dashboard/edit 等路由

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
