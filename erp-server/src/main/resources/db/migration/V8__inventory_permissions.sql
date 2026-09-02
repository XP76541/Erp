-- ============================================================
-- 库存管理模块权限配置
-- 文件: V8__inventory_permissions.sql
-- 描述: 添加库存管理相关的菜单和按钮权限
-- ============================================================

-- 插入库存管理权限。先插入菜单层级，再插入按钮权限，避免父级子查询返回 NULL。
DECLARE @inventoryId BIGINT;
DECLARE @transferId BIGINT;
DECLARE @checkId BIGINT;
DECLARE @warningId BIGINT;

INSERT INTO sys_permission (code, name, type, parent_id, sort)
VALUES ('inventory', '库存管理', 'MENU', 0, 9);
SELECT @inventoryId = id FROM sys_permission WHERE code = 'inventory';

INSERT INTO sys_permission (code, name, type, parent_id, sort) VALUES
('inventory:transfer', '库存调拨', 'MENU', @inventoryId, 1),
('inventory:check', '库存盘点', 'MENU', @inventoryId, 2),
('inventory:warning', '库存预警', 'MENU', @inventoryId, 3);
SELECT @transferId = id FROM sys_permission WHERE code = 'inventory:transfer';
SELECT @checkId = id FROM sys_permission WHERE code = 'inventory:check';
SELECT @warningId = id FROM sys_permission WHERE code = 'inventory:warning';

INSERT INTO sys_permission (code, name, type, parent_id, sort) VALUES
('inventory:transfer:view', '查看调拨单', 'BUTTON', @transferId, 1),
('inventory:transfer:create', '创建调拨单', 'BUTTON', @transferId, 2),
('inventory:transfer:update', '修改调拨单', 'BUTTON', @transferId, 3),
('inventory:transfer:audit', '审核调拨单', 'BUTTON', @transferId, 4),
('inventory:transfer:complete', '完成调拨', 'BUTTON', @transferId, 5),
('inventory:transfer:cancel', '取消调拨', 'BUTTON', @transferId, 6),
('inventory:transfer:print', '打印调拨单', 'BUTTON', @transferId, 7),
('inventory:check:view', '查看盘点单', 'BUTTON', @checkId, 1),
('inventory:check:create', '创建盘点单', 'BUTTON', @checkId, 2),
('inventory:check:update', '修改盘点单', 'BUTTON', @checkId, 3),
('inventory:check:start-check', '开始盘点', 'BUTTON', @checkId, 4),
('inventory:check:submit-result', '提交盘点结果', 'BUTTON', @checkId, 5),
('inventory:check:audit', '审核盘点单', 'BUTTON', @checkId, 6),
('inventory:check:cancel', '取消盘点', 'BUTTON', @checkId, 7),
('inventory:check:print', '打印盘点单', 'BUTTON', @checkId, 8),
('inventory:warning:view', '查看预警', 'BUTTON', @warningId, 1),
('inventory:warning:create', '创建预警', 'BUTTON', @warningId, 2),
('inventory:warning:update', '修改预警', 'BUTTON', @warningId, 3),
('inventory:warning:resolve', '解决预警', 'BUTTON', @warningId, 4),
('inventory:warning:batch-resolve', '批量解决', 'BUTTON', @warningId, 5),
('inventory:warning:config', '预警配置', 'BUTTON', @warningId, 6),
('inventory:warning-config:view', '查看预警配置', 'BUTTON', @warningId, 7),
('inventory:warning-config:create', '创建预警配置', 'BUTTON', @warningId, 8),
('inventory:warning-config:update', '修改预警配置', 'BUTTON', @warningId, 9),
('inventory:warning-config:toggle', '启用/禁用配置', 'BUTTON', @warningId, 10),
('inventory:warning-config:batch-toggle', '批量启用/禁用', 'BUTTON', @warningId, 11),
('inventory:warning-config:print', '打印预警配置', 'BUTTON', @warningId, 12);
GO
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