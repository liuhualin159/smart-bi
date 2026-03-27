-- ----------------------------
-- Agent BI V2 反馈管理菜单
-- 依赖：V7 已添加 bi:feedback:approve 权限
-- ----------------------------

-- 在智能BI(2000)下新增「反馈管理」菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '反馈管理', 2000, 6, 'feedback', 'feedback/index', NULL, '', 1, 0, 'C', '0', '0', 'bi:feedback:approve', 'message', 'admin', sysdate(), '管理员审核用户反馈，通过后写入 bi_feedback_correction'
FROM (SELECT 1) AS t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '反馈管理' AND parent_id = 2000);
