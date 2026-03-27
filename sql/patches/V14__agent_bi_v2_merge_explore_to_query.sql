-- ----------------------------
-- 方案 B：合并数据浏览到智能问数，移除独立 explore 菜单
-- ----------------------------

SET @bi_parent = (SELECT menu_id FROM sys_menu WHERE menu_name = '智能BI' AND parent_id = 0 LIMIT 1);

DELETE FROM sys_menu WHERE path = 'explore' AND parent_id = @bi_parent;
