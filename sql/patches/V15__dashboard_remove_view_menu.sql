-- 移除「查看看板」隐藏菜单：查看改为在新页签打开设计器预览，不再使用独立查看页
DELETE FROM sys_menu WHERE component = 'dashboard/view';
