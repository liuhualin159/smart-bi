-- ----------------------------
-- Agent BI V2 数据浏览菜单
-- ----------------------------

SET @bi_parent = (SELECT menu_id FROM sys_menu WHERE menu_name = '智能BI' AND parent_id = 0 LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据浏览', @bi_parent, 6, 'explore', 'explore/index', NULL, '', 1, 0, 'C', '0', '0', 'metadata:table:list', 'tree-table', 'admin', sysdate(), '数据浏览、表详情、预览、分析模板库'
FROM (SELECT 1) AS t WHERE @bi_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = 'explore' AND parent_id = @bi_parent);
