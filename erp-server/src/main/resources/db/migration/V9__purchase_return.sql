-- 采购退货(F203):草稿审核后按原入库成本出库，并生成红字应付调整
CREATE TABLE purchase_return (
    id          BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    doc_no      NVARCHAR(30) NOT NULL,
    supplier_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    biz_date    DATE NOT NULL,
    status      NVARCHAR(10) NOT NULL DEFAULT N'DRAFT',
    reason      NVARCHAR(500) NOT NULL DEFAULT N'',
    audit_by    BIGINT NULL,
    audit_at    DATETIME2 NULL,
    created_by  BIGINT NULL,
    created_at  DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_by  BIGINT NULL,
    updated_at  DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE UNIQUE INDEX uk_pr_no ON purchase_return (doc_no);
GO
CREATE INDEX idx_pr_supplier ON purchase_return (supplier_id, biz_date);
GO

CREATE TABLE purchase_return_item (
    id               BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    return_id        BIGINT NOT NULL,
    line_no          INT NOT NULL,
    inbound_item_id  BIGINT NOT NULL,
    product_id       BIGINT NOT NULL,
    warehouse_id     BIGINT NOT NULL,
    qty              DECIMAL(18,4) NOT NULL,
    unit_cost        DECIMAL(18,2) NOT NULL,
    amount           DECIMAL(18,2) NOT NULL,
    note             NVARCHAR(200) NOT NULL DEFAULT N''
);
GO
CREATE UNIQUE INDEX uk_pri_line ON purchase_return_item (return_id, line_no);
GO
CREATE INDEX idx_pri_source ON purchase_return_item (inbound_item_id);
GO

-- 应付来源唯一，避免审核重试产生重复账务
CREATE UNIQUE INDEX uk_payable_source ON payable (doc_type, doc_id);
GO

DECLARE @purchaseId BIGINT;
INSERT INTO sys_permission (code, name, type, parent_id, sort)
SELECT 'purchase:return', '采购退货', 'MENU', id, 3
FROM sys_permission WHERE code = 'purchase';
SELECT @purchaseId = id FROM sys_permission WHERE code = 'purchase:return';
INSERT INTO sys_permission (code, name, type, parent_id, sort) VALUES
('purchase:return:view', '查看采购退货', 'BUTTON', @purchaseId, 1),
('purchase:return:create', '创建采购退货', 'BUTTON', @purchaseId, 2),
('purchase:return:audit', '审核采购退货', 'BUTTON', @purchaseId, 3);
GO
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE code LIKE 'purchase:return%'
AND id NOT IN (SELECT permission_id FROM sys_role_permission WHERE role_id = 1);
GO
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE code IN ('purchase:return:view','purchase:return:create','purchase:return:audit')
AND id NOT IN (SELECT permission_id FROM sys_role_permission WHERE role_id = 3);
GO
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission WHERE code = 'purchase:return:view'
AND id NOT IN (SELECT permission_id FROM sys_role_permission WHERE role_id = 4);
GO
