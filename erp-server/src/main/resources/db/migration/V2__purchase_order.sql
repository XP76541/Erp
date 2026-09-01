-- ============================================================
-- 采购订单模块表结构(SQL Server / T-SQL)
-- 文件: V2__purchase_order.sql
-- 描述: 添加采购订单相关表结构
-- ============================================================

-- ------------------------------------------------------------
-- 采购订单
-- ------------------------------------------------------------
CREATE TABLE purchase_order (
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    doc_no        NVARCHAR(30)  NOT NULL, -- POyyyyMMdd-nnnn,草稿即取号
    supplier_id   BIGINT        NOT NULL,
    warehouse_id  BIGINT        NOT NULL,
    biz_date      DATE          NOT NULL, -- 业务日期
    status        NVARCHAR(10)  NOT NULL DEFAULT N'DRAFT', -- DRAFT/AUDITED/VOID
    total_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0, -- 总金额
    remark        NVARCHAR(500)  NOT NULL DEFAULT N'',
    created_by    BIGINT        NULL,
    created_at    DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    updated_by    BIGINT        NULL,
    updated_at    DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    audit_by      BIGINT        NULL,     -- 审核人ID
    audit_at      DATETIME2     NULL,     -- 审核时间
    reject_by     BIGINT        NULL,     -- 驳回人ID
    reject_at     DATETIME2     NULL      -- 驳回时间
);
GO
CREATE UNIQUE INDEX uk_po_no ON purchase_order (doc_no);
GO
CREATE INDEX idx_po_supplier ON purchase_order (supplier_id, biz_date);
GO
CREATE INDEX idx_po_warehouse ON purchase_order (warehouse_id, biz_date);
GO
CREATE INDEX idx_po_status ON purchase_order (status, biz_date);
GO

-- ------------------------------------------------------------
-- 采购订单明细
-- ------------------------------------------------------------
CREATE TABLE purchase_order_item (
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    order_id      BIGINT        NOT NULL,
    line_no       INT           NOT NULL, -- 行号,从1开始
    product_id    BIGINT        NOT NULL,
    qty           DECIMAL(18, 4) NOT NULL, -- 采购数量
    price         DECIMAL(18, 2) NOT NULL, -- 单价
    amount        DECIMAL(18, 2) NOT NULL, -- 金额=qty*price,服务端计算
    received_qty  DECIMAL(18, 4) NOT NULL DEFAULT 0, -- 已入库数量
    note          NVARCHAR(200)  NOT NULL DEFAULT N'',
    created_at    DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    updated_at    DATETIME2     NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE INDEX poi_order ON purchase_order_item (order_id);
GO
CREATE INDEX poi_product ON purchase_order_item (product_id);
GO

-- ------------------------------------------------------------
-- 采购订单状态变更日志
-- ------------------------------------------------------------
CREATE TABLE purchase_order_log (
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    order_id      BIGINT        NOT NULL,
    from_status   NVARCHAR(10)  NOT NULL,
    to_status     NVARCHAR(10)  NOT NULL,
    operation     NVARCHAR(20)  NOT NULL, -- CREATE/AUDIT/REJECT/UPDATE
    operator_id   BIGINT        NOT NULL,
    operator_name  NVARCHAR(50) NOT NULL,
    remark        NVARCHAR(500) NOT NULL DEFAULT N'',
    created_at    DATETIME2     NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE INDEX pol_order ON purchase_order_log (order_id);
GO
CREATE INDEX pol_status ON purchase_order_log (from_status, to_status);
GO
CREATE INDEX pol_operator ON purchase_order_log (operator_id);
GO

-- ------------------------------------------------------------
-- 初始化采购订单文档序列
-- ------------------------------------------------------------
INSERT INTO doc_sequence (doc_type, period, next_no) VALUES
    ('PO', 'ALL', 1); -- PO编号从1开始，格式: POyyyyMMdd-nnnn
GO