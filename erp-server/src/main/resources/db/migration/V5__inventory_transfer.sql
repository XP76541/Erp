-- ============================================================
-- 库存调拨表结构
-- 文件: V5__inventory_transfer.sql
-- 描述: 创建库存调拨相关的表结构
-- ============================================================

-- 创建库存调拨主表
CREATE TABLE inventory_transfer (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    doc_no NVARCHAR(30) NOT NULL,
    from_warehouse_id BIGINT NOT NULL,
    to_warehouse_id BIGINT NOT NULL,
    biz_date DATE NOT NULL,
    status NVARCHAR(10) NOT NULL DEFAULT N'DRAFT', -- DRAFT/AUDITED/COMPLETED/CANCELLED
    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    operator_id BIGINT NOT NULL,
    audit_by BIGINT NULL,
    audit_at DATETIME2 NULL,
    approved_by BIGINT NULL,
    approved_at DATETIME2 NULL,
    remark NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

-- 创建库存调拨明细表
CREATE TABLE inventory_transfer_item (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    product_id BIGINT NOT NULL,
    from_warehouse_id BIGINT NOT NULL,
    to_warehouse_id BIGINT NOT NULL,
    qty DECIMAL(18, 3) NOT NULL,
    price DECIMAL(18, 2) NOT NULL DEFAULT 0,
    amount DECIMAL(18, 2) NOT NULL,
    note NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

-- 创建库存调拨状态变更日志表
CREATE TABLE inventory_transfer_log (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    old_status NVARCHAR(10) NOT NULL,
    new_status NVARCHAR(10) NOT NULL,
    operator_id BIGINT NOT NULL,
    operation_time DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    remark NVARCHAR(500) NULL
);
GO

-- 添加外键约束
ALTER TABLE inventory_transfer_item
    ADD CONSTRAINT fk_transfer_item_transfer FOREIGN KEY (transfer_id) REFERENCES inventory_transfer(id);
GO

-- 添加索引
CREATE INDEX idx_transfer_doc ON inventory_transfer (doc_no);
CREATE INDEX idx_transfer_warehouse ON inventory_transfer (from_warehouse_id, to_warehouse_id);
CREATE INDEX idx_transfer_status ON inventory_transfer (status);
CREATE INDEX idx_transfer_item_product ON inventory_transfer_item (product_id);
GO

-- 创建库存调拨文档序列
INSERT INTO sys_doc_sequence (code, name, prefix, cycle, length, current_year, current_number)
VALUES
    ('TRANSFER', '库存调拨单', 'TF', 'YEAR', 8, YEAR(GETDATE()), 1);
GO

-- 更新现有文档序列
UPDATE sys_doc_sequence
SET current_number = current_number + 1
WHERE code = 'TRANSFER';
GO