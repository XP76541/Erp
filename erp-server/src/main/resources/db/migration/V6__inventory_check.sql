-- ============================================================
-- 库存盘点表结构
-- 文件: V6__inventory_check.sql
-- 描述: 创建库存盘点相关的表结构
-- ============================================================

-- 创建库存盘点主表
CREATE TABLE inventory_check (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    doc_no NVARCHAR(30) NOT NULL,
    warehouse_id BIGINT NOT NULL,
    check_date DATE NOT NULL,
    status NVARCHAR(10) NOT NULL DEFAULT N'DRAFT', -- DRAFT/AUDITING/AUDITED/CANCELLED
    check_type NVARCHAR(10) NOT NULL DEFAULT N'FULL', -- FULL/PARTIAL
    total_items INT NOT NULL DEFAULT 0,
    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    diff_items INT NOT NULL DEFAULT 0,
    diff_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    operator_id BIGINT NOT NULL,
    audit_by BIGINT NULL,
    audit_at DATETIME2 NULL,
    remark NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

-- 创建库存盘点明细表
CREATE TABLE inventory_check_item (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    check_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    system_qty DECIMAL(18, 3) NOT NULL,
    actual_qty DECIMAL(18, 3) NOT NULL,
    diff_qty DECIMAL(18, 3) NOT NULL,
    price DECIMAL(18, 2) NOT NULL DEFAULT 0,
    amount DECIMAL(18, 2) NOT NULL,
    status NVARCHAR(10) NOT NULL DEFAULT N'NORMAL', -- NORMAL/DIFF/MISSING/EXCESS
    note NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

-- 创建库存盘点状态变更日志表
CREATE TABLE inventory_check_log (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    check_id BIGINT NOT NULL,
    old_status NVARCHAR(10) NOT NULL,
    new_status NVARCHAR(10) NOT NULL,
    operator_id BIGINT NOT NULL,
    operation_time DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    remark NVARCHAR(500) NULL
);
GO

-- 添加外键约束
ALTER TABLE inventory_check_item
    ADD CONSTRAINT fk_check_item_check FOREIGN KEY (check_id) REFERENCES inventory_check(id);
GO

-- 添加索引
CREATE INDEX idx_check_doc ON inventory_check (doc_no);
CREATE INDEX idx_check_warehouse ON inventory_check (warehouse_id);
CREATE INDEX idx_check_status ON inventory_check (status);
CREATE INDEX idx_check_item_product ON inventory_check_item (product_id);
CREATE INDEX idx_check_item_warehouse ON inventory_check_item (warehouse_id);
GO

-- 创建库存盘点文档序列
IF NOT EXISTS (SELECT 1 FROM doc_sequence WHERE doc_type = 'CHK' AND period = 'ALL')
BEGIN
    INSERT INTO doc_sequence (doc_type, period, next_no)
    VALUES ('CHK', 'ALL', 1);
END;
GO