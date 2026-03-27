-- ----------------------------
-- Agent BI 数据源管理菜单配置
-- ----------------------------

-- 一级菜单：智能BI
insert into sys_menu values('2000', '智能BI', '0', '5', 'bi', null, '', '', 1, 0, 'M', '0', '0', '', 'chart', 'admin', sysdate(), '', null, '智能BI目录');

-- 二级菜单：数据源管理
insert into sys_menu values('2001', '数据源管理', '2000', '1', 'datasource', 'datasource/index', '', '', 1, 0, 'C', '0', '0', 'datasource:list', 'database', 'admin', sysdate(), '', null, '数据源管理菜单');

-- 按钮权限
-- 数据源查询
insert into sys_menu values('2100', '数据源查询', '2001', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:query', '#', 'admin', sysdate(), '', null, '');

-- 数据源新增
insert into sys_menu values('2101', '数据源新增', '2001', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:add', '#', 'admin', sysdate(), '', null, '');

-- 数据源修改
insert into sys_menu values('2102', '数据源修改', '2001', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:edit', '#', 'admin', sysdate(), '', null, '');

-- 数据源删除
insert into sys_menu values('2103', '数据源删除', '2001', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:remove', '#', 'admin', sysdate(), '', null, '');

-- 数据源测试
insert into sys_menu values('2104', '数据源测试', '2001', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:test', '#', 'admin', sysdate(), '', null, '');

-- 二级菜单：ETL任务管理
insert into sys_menu values('2002', 'ETL任务管理', '2000', '2', 'etl', 'etl/index', '', '', 1, 0, 'C', '0', '0', 'etl:task:list', 'list', 'admin', sysdate(), '', null, 'ETL任务管理菜单');

-- 按钮权限
-- ETL任务查询
insert into sys_menu values('2200', 'ETL任务查询', '2002', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'etl:task:query', '#', 'admin', sysdate(), '', null, '');

-- ETL任务新增
insert into sys_menu values('2201', 'ETL任务新增', '2002', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'etl:task:add', '#', 'admin', sysdate(), '', null, '');

-- ETL任务修改
insert into sys_menu values('2202', 'ETL任务修改', '2002', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'etl:task:edit', '#', 'admin', sysdate(), '', null, '');

-- ETL任务删除
insert into sys_menu values('2203', 'ETL任务删除', '2002', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'etl:task:remove', '#', 'admin', sysdate(), '', null, '');

-- ETL任务触发
insert into sys_menu values('2204', 'ETL任务触发', '2002', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'etl:task:trigger', '#', 'admin', sysdate(), '', null, '');

-- 二级菜单：元数据管理
insert into sys_menu values('2003', '元数据管理', '2000', '3', 'metadata', 'metadata/index', '', '', 1, 0, 'C', '0', '0', 'metadata:table:list', 'tree-table', 'admin', sysdate(), '', null, '元数据管理菜单');

-- 按钮权限
-- 业务域管理
insert into sys_menu values('2300', '业务域查询', '2003', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'metadata:domain:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2301', '业务域新增', '2003', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'metadata:domain:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2302', '业务域修改', '2003', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'metadata:domain:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2303', '业务域删除', '2003', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'metadata:domain:remove', '#', 'admin', sysdate(), '', null, '');

-- 表元数据管理
insert into sys_menu values('2304', '表元数据查询', '2003', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'metadata:table:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2305', '表元数据新增', '2003', '6', '', '', '', '', 1, 0, 'F', '0', '0', 'metadata:table:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2306', '表元数据修改', '2003', '7', '', '', '', '', 1, 0, 'F', '0', '0', 'metadata:table:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2307', '表元数据删除', '2003', '8', '', '', '', '', 1, 0, 'F', '0', '0', 'metadata:table:remove', '#', 'admin', sysdate(), '', null, '');

-- 字段元数据管理
insert into sys_menu values('2308', '字段元数据查询', '2003', '9', '', '', '', '', 1, 0, 'F', '0', '0', 'metadata:field:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2309', '字段元数据新增', '2003', '10', '', '', '', '', 1, 0, 'F', '0', '0', 'metadata:field:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2310', '字段元数据修改', '2003', '11', '', '', '', '', 1, 0, 'F', '0', '0', 'metadata:field:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2311', '字段元数据删除', '2003', '12', '', '', '', '', 1, 0, 'F', '0', '0', 'metadata:field:remove', '#', 'admin', sysdate(), '', null, '');

-- 二级菜单：智能问数
insert into sys_menu values('2004', '智能问数', '2000', '4', 'query', 'query/index', '', '', 1, 0, 'C', '0', '0', 'query:execute', 'search', 'admin', sysdate(), '', null, '智能问数菜单');

-- 按钮权限
-- 智能问数执行
insert into sys_menu values('2400', '智能问数执行', '2004', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'query:execute', '#', 'admin', sysdate(), '', null, '');

-- 智能问数历史
insert into sys_menu values('2401', '智能问数历史', '2004', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'query:list', '#', 'admin', sysdate(), '', null, '');

-- 二级菜单：看板管理
insert into sys_menu values('2005', '看板管理', '2000', '5', 'dashboard', 'dashboard/index', '', '', 1, 0, 'C', '0', '0', 'dashboard:list', 'dashboard', 'admin', sysdate(), '', null, '看板管理菜单');

-- 按钮权限
-- 看板查询
insert into sys_menu values('2500', '看板查询', '2005', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'dashboard:query', '#', 'admin', sysdate(), '', null, '');

-- 看板新增
insert into sys_menu values('2501', '看板新增', '2005', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'dashboard:add', '#', 'admin', sysdate(), '', null, '');

-- 看板修改
insert into sys_menu values('2502', '看板修改', '2005', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'dashboard:edit', '#', 'admin', sysdate(), '', null, '');

-- 看板删除
insert into sys_menu values('2503', '看板删除', '2005', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'dashboard:remove', '#', 'admin', sysdate(), '', null, '');

-- 看板刷新
insert into sys_menu values('2504', '看板刷新', '2005', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'dashboard:refresh', '#', 'admin', sysdate(), '', null, '');
