-- ============================================================
-- 库存管理模块权限配置
-- 文件: V8__inventory_permissions.sql
-- 描述: 添加库存管理相关的菜单和按钮权限
-- ============================================================

-- 插入库存管理模块权限
INSERT INTO sys_permission (code, name, type, parent_id, sort) VALUES
-- 库存管理菜单
('inventory', '库存管理', 'MENU', 0, 9),

-- 库存调拨菜单
('inventory:transfer', '库存调拨', 'MENU', (SELECT id FROM sys_permission WHERE code = 'inventory'), 1),
-- 库存盘点菜单
('inventory:check', '库存盘点', 'MENU', (SELECT id FROM sys_permission WHERE code = 'inventory'), 2),
-- 库存预警菜单
('inventory:warning', '库存预警', 'MENU', (SELECT id FROM sys_permission WHERE code = 'inventory'), 3),

-- 库存调拨权限
('inventory:transfer:view', '查看调拨单', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:transfer'), 1),
('inventory:transfer:create', '创建调拨单', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:transfer'), 2),
('inventory:transfer:update', '修改调拨单', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:transfer'), 3),
('inventory:transfer:audit', '审核调拨单', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:transfer'), 4),
('inventory:transfer:complete', '完成调拨', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:transfer'), 5),
('inventory:transfer:cancel', '取消调拨', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:transfer'), 6),
('inventory:transfer:print', '打印调拨单', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:transfer'), 7),

-- 库存盘点权限
('inventory:check:view', '查看盘点单', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:check'), 1),
('inventory:check:create', '创建盘点单', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:check'), 2),
('inventory:check:update', '修改盘点单', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:check'), 3),
('inventory:check:start-check', '开始盘点', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:check'), 4),
('inventory:check:submit-result', '提交盘点结果', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:check'), 5),
('inventory:check:audit', '审核盘点单', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:check'), 6),
('inventory:check:cancel', '取消盘点', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:check'), 7),
('inventory:check:print', '打印盘点单', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:check'), 8),

-- 库存预警权限
('inventory:warning:view', '查看预警', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:warning'), 1),
('inventory:warning:create', '创建预警', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:warning'), 2),
('inventory:warning:update', '修改预警', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:warning'), 3),
('inventory:warning:resolve', '解决预警', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:warning'), 4),
('inventory:warning:batch-resolve', '批量解决', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:warning'), 5),
('inventory:warning:config', '预警配置', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:warning'), 6),

-- 库存预警配置权限
('inventory:warning-config:view', '查看预警配置', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:warning'), 7),
('inventory:warning-config:create', '创建预警配置', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:warning'), 8),
('inventory:warning-config:update', '修改预警配置', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:warning'), 9),
('inventory:warning-config:toggle', '启用/禁用配置', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:warning'), 10),
('inventory:warning-config:batch-toggle', '批量启用/禁用', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:warning'), 11),
('inventory:warning-config:print', '打印预警配置', 'BUTTON', (SELECT id FROM sys_permission WHERE code = 'inventory:warning'), 12);
GO

-- 为管理员角色分配所有库存相关权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
WHERE code LIKE 'inventory%'
AND id NOT IN (SELECT permission_id FROM sys_role_permission WHERE role_id = 1);
GO

-- 为仓库管理员角色分配库存相关权限（除配置外）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission
WHERE code LIKE 'inventory:%'
AND (code LIKE '%:view' OR code LIKE '%:create' OR code LIKE '%:update' OR
     code LIKE '%:audit' OR code LIKE '%:start-check' OR code LIKE '%:submit-result' OR
     code LIKE '%:complete' OR code LIKE '%:resolve')
AND id NOT IN (SELECT permission_id FROM sys_role_permission WHERE role_id = 3);
GO

-- 为财务角色分配库存相关权限（仅查看）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission
WHERE code LIKE 'inventory:%'
AND (code LIKE '%:view' OR code = 'inventory:warning:resolve')
AND id NOT IN (SELECT permission_id FROM sys_role_permission WHERE role_id = 4);
GO

-- 为普通员工角色分配基础库存查看权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission
WHERE code LIKE 'inventory:%'
AND code LIKE '%:view'
AND id NOT IN (SELECT permission_id FROM sys_role_permission WHERE role_id = 2);
GO