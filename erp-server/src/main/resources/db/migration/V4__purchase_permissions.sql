-- ============================================================
-- 采购订单模块权限配置
-- 文件: V4__purchase_permissions.sql
-- 描述: 添加采购订单相关的菜单和按钮权限
-- ============================================================

-- 插入采购订单模块权限。SQL Server 不支持在同一多行 INSERT 中可靠引用刚插入行，
-- 因此先插入父菜单，再通过变量插入子菜单和按钮权限。
DECLARE @purchaseId BIGINT;
DECLARE @purchaseOrderId BIGINT;
DECLARE @purchaseInboundId BIGINT;

INSERT INTO sys_permission (code, name, type, parent_id, sort)
VALUES ('purchase', '采购管理', 'MENU', 0, 8);
SELECT @purchaseId = id FROM sys_permission WHERE code = 'purchase';

INSERT INTO sys_permission (code, name, type, parent_id, sort)
VALUES ('purchase:order', '采购订单', 'MENU', @purchaseId, 1);
SELECT @purchaseOrderId = id FROM sys_permission WHERE code = 'purchase:order';

INSERT INTO sys_permission (code, name, type, parent_id, sort)
VALUES ('purchase:inbound', '采购入库', 'MENU', @purchaseId, 2);
SELECT @purchaseInboundId = id FROM sys_permission WHERE code = 'purchase:inbound';

INSERT INTO sys_permission (code, name, type, parent_id, sort) VALUES
('purchase:order:view', '查看采购订单', 'BUTTON', @purchaseOrderId, 1),
('purchase:order:create', '创建采购订单', 'BUTTON', @purchaseOrderId, 2),
('purchase:order:update', '修改采购订单', 'BUTTON', @purchaseOrderId, 3),
('purchase:order:audit', '审核采购订单', 'BUTTON', @purchaseOrderId, 4),
('purchase:order:reject', '驳回采购订单', 'BUTTON', @purchaseOrderId, 5),
('purchase:order:delete', '删除采购订单', 'BUTTON', @purchaseOrderId, 6),
('purchase:order:print', '打印采购订单', 'BUTTON', @purchaseOrderId, 7),
('purchase:order:create-inbound', '创建入库单', 'BUTTON', @purchaseOrderId, 8),
('purchase:order:receive', '入库确认', 'BUTTON', @purchaseOrderId, 9),
('purchase:inbound:view', '查看入库单', 'BUTTON', @purchaseInboundId, 1),
('purchase:inbound:create', '创建入库单', 'BUTTON', @purchaseInboundId, 2),
('purchase:inbound:update', '修改入库单', 'BUTTON', @purchaseInboundId, 3),
('purchase:inbound:audit', '审核入库单', 'BUTTON', @purchaseInboundId, 4),
('purchase:inbound:reject', '驳回入库单', 'BUTTON', @purchaseInboundId, 5),
('purchase:inbound:delete', '删除入库单', 'BUTTON', @purchaseInboundId, 6),
('purchase:inbound:print', '打印入库单', 'BUTTON', @purchaseInboundId, 7);

-- 为管理员角色分配所有采购相关权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
WHERE code LIKE 'purchase%'
AND id NOT IN (SELECT permission_id FROM sys_role_permission WHERE role_id = 1);
GO

-- 为财务角色分配采购相关权限（除创建和修改外）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission
WHERE code LIKE 'purchase:%'
AND (code LIKE '%:view' OR code LIKE '%:audit' OR code LIKE '%:print' OR code LIKE '%:receive' OR code = 'purchase:inbound:audit')
AND id NOT IN (SELECT permission_id FROM sys_role_permission WHERE role_id = 4);
GO

-- 为仓管角色分配采购入库相关权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission
WHERE code LIKE 'purchase:inbound%'
AND (code LIKE '%:view' OR code LIKE '%:create' OR code LIKE '%:update' OR code LIKE '%:audit' OR code LIKE '%:print' OR code LIKE '%:receive')
AND id NOT IN (SELECT permission_id FROM sys_role_permission WHERE role_id = 3);
GO