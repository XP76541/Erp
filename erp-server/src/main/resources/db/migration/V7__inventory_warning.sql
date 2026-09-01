-- ============================================================
-- 库存预警表结构
-- 文件: V7__inventory_warning.sql
-- 描述: 创建库存预警相关的表结构
-- ============================================================

-- 创建库存预警主表
CREATE TABLE inventory_warning (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    warning_type NVARCHAR(20) NOT NULL, -- STOCK_OUT/STOCK_OVER/EXPIRING/SPOILED
    warehouse_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    current_qty DECIMAL(18, 3) NOT NULL,
    warning_value DECIMAL(18, 3) NOT NULL,
    is_active BIT NOT NULL DEFAULT 1,
    resolved_at DATETIME2 NULL,
    resolved_by BIGINT NULL,
    remark NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

-- 创建库存预警日志表
CREATE TABLE inventory_warning_log (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    warning_id BIGINT NOT NULL,
    old_qty DECIMAL(18, 3) NOT NULL,
    new_qty DECIMAL(18, 3) NOT NULL,
    operator_id BIGINT NOT NULL,
    operation_time DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    operation_type NVARCHAR(20) NOT NULL, -- STOCK_IN/STOCK_OUT/TRANSFER
    remark NVARCHAR(500) NULL
);
GO

-- 创建库存预警配置表
CREATE TABLE inventory_warning_config (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    stock_out_limit DECIMAL(18, 3) NOT NULL DEFAULT 0, -- 库存下限
    stock_over_limit DECIMAL(18, 3) NOT NULL,         -- 库存上限
    warning_level NVARCHAR(10) NOT NULL DEFAULT N'NORMAL', -- LOW/MEDIUM/HIGH
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT uk_warning_config UNIQUE (product_id, warehouse_id)
);
GO

-- 添加外键约束
ALTER TABLE inventory_warning_log
    ADD CONSTRAINT fk_warning_log_warning FOREIGN KEY (warning_id) REFERENCES inventory_warning(id);
GO

-- 添加索引
CREATE INDEX idx_warning_type ON inventory_warning (warning_type);
CREATE INDEX idx_warning_warehouse ON inventory_warning (warehouse_id);
CREATE INDEX idx_warning_product ON inventory_warning (product_id);
CREATE INDEX idx_warning_active ON inventory_warning (is_active);
CREATE INDEX idx_warning_log_warning ON inventory_warning_log (warning_id);
CREATE INDEX idx_warning_config_product ON inventory_warning_config (product_id);
CREATE INDEX idx_warning_config_warehouse ON inventory_warning_config (warehouse_id);
GO