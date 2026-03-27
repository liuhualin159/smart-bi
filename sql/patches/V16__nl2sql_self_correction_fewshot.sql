-- NL2SQL 自动修正与 Few-shot 示例库
-- 1. bi_llm_audit 表增加重试次数和最终SQL字段
ALTER TABLE bi_llm_audit ADD COLUMN retry_count INT DEFAULT 0 COMMENT '自修正重试次数';
ALTER TABLE bi_llm_audit ADD COLUMN final_sql TEXT COMMENT '最终执行的SQL（修正后可能与generated_sql不同）';

-- 2. bi_fewshot_example 表（Few-shot 示例库）
-- embedding 向量存储在 Qdrant，表中不含 embedding 列
CREATE TABLE IF NOT EXISTS bi_fewshot_example (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    question    TEXT        NOT NULL COMMENT '示例问题',
    sql_text    TEXT        NOT NULL COMMENT '示例SQL',
    datasource_id BIGINT   NULL COMMENT '关联数据源ID，NULL表示通用',
    domain_tags VARCHAR(500) NULL COMMENT '领域标签，逗号分隔',
    enabled     TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    created_by  BIGINT      NULL COMMENT '创建人ID',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_datasource (datasource_id),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Few-shot示例库';

-- 3. 菜单：在元数据模块下新增 Few-shot 示例管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'Few-shot示例', menu_id, 6, 'fewshot', 'metadata/fewshot/index', 'C', '0', '0', 'metadata:fewshot:list', 'example', 'admin', NOW(), 'Few-shot示例管理'
FROM sys_menu WHERE path = 'metadata' AND menu_type = 'M' LIMIT 1;

-- 4. 菜单按钮权限
SET @fewshotMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'metadata:fewshot:list' LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES
    ('查询', @fewshotMenuId, 1, '', '', 'F', '0', '0', 'metadata:fewshot:query', '#', 'admin', NOW()),
    ('新增', @fewshotMenuId, 2, '', '', 'F', '0', '0', 'metadata:fewshot:add', '#', 'admin', NOW()),
    ('修改', @fewshotMenuId, 3, '', '', 'F', '0', '0', 'metadata:fewshot:edit', '#', 'admin', NOW()),
    ('删除', @fewshotMenuId, 4, '', '', 'F', '0', '0', 'metadata:fewshot:remove', '#', 'admin', NOW()),
    ('导入', @fewshotMenuId, 5, '', '', 'F', '0', '0', 'metadata:fewshot:import', '#', 'admin', NOW());
