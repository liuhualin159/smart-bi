-- ----------------------------
-- 修复订阅管理菜单 path='index' 导致路由名称 'Index' 与首页冲突的问题
-- 将 path 从 'index' 改为 'list'，并显式设置 route_name 防止后续冲突
-- ----------------------------

UPDATE sys_menu
SET path       = 'list',
    route_name = 'SubscriptionList'
WHERE perms = 'bi:subscription:list'
  AND path = 'index';
