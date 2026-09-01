-- ============================================================
-- 小型贸易公司 ERP 一期建表脚本(SQL Server / T-SQL)
-- 依据:docs/database-design.md v0.1
-- 要求:SQL Server 2016+(Express 可用)
-- 数据库需使用中文排序规则,创建语句:
--   CREATE DATABASE erp COLLATE Chinese_PRC_CI_AS;
-- 约定:档案表用 is_active 停用;单据表不删除,走 status 状态机
-- 注意:SQL Server 无 ON UPDATE CURRENT_TIMESTAMP,updated_at 由应用层
--       (AuditMetaHandler)统一填充;无 JSON 类型,用 NVARCHAR(MAX)
-- ============================================================

-- ------------------------------------------------------------
-- 系统管理
-- ------------------------------------------------------------

-- 用户
CREATE TABLE sys_user (
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    username      NVARCHAR(30)  NOT NULL,           -- 登录名
    password_hash NVARCHAR(100) NOT NULL,           -- BCrypt 哈希,禁明文
    real_name     NVARCHAR(50)  NOT NULL DEFAULT N'', -- 姓名
    is_active     TINYINT       NOT NULL DEFAULT 1, -- 1 启用 0 禁用
    created_by    BIGINT        NULL,
    created_at    DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    updated_by    BIGINT        NULL,
    updated_at    DATETIME2     NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE UNIQUE INDEX uk_user_username ON sys_user (username);
GO

-- 角色
CREATE TABLE sys_role (
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code       NVARCHAR(30)  NOT NULL, -- 角色编码:ADMIN/SALES/WAREHOUSE/FINANCE
    name       NVARCHAR(50)  NOT NULL, -- 角色名称
    remark     NVARCHAR(200) NOT NULL DEFAULT N'',
    created_at DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2     NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE UNIQUE INDEX uk_role_code ON sys_role (code);
GO

-- 菜单与按钮权限点
CREATE TABLE sys_permission (
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code       NVARCHAR(50) NOT NULL, -- 权限点,如 sales:order:audit
    name       NVARCHAR(50) NOT NULL,
    type       NVARCHAR(10) NOT NULL DEFAULT N'MENU', -- MENU 菜单 / BUTTON 按钮
    parent_id  BIGINT       NOT NULL DEFAULT 0,       -- 上级,0 为根
    sort       INT          NOT NULL DEFAULT 0,
    created_at DATETIME2    NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2    NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE UNIQUE INDEX uk_permission_code ON sys_permission (code);
GO

-- 用户-角色
CREATE TABLE sys_user_role (
    id      BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL
);
GO
CREATE UNIQUE INDEX uk_user_role ON sys_user_role (user_id, role_id);
GO

-- 角色-权限点
CREATE TABLE sys_role_permission (
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL
);
GO
CREATE UNIQUE INDEX uk_role_permission ON sys_role_permission (role_id, permission_id);
GO

-- 操作日志(只增不改)
CREATE TABLE operation_log (
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id    BIGINT       NOT NULL, -- 操作人
    user_name  NVARCHAR(50) NOT NULL, -- 操作人姓名快照
    module     NVARCHAR(30) NOT NULL, -- 模块,如 sales_order
    action     NVARCHAR(30) NOT NULL, -- 动作:AUDIT/VOID/PRICE_CHANGE...
    doc_type   NVARCHAR(20) NULL,     -- 目标单据类型
    doc_id     BIGINT       NULL,
    doc_no     NVARCHAR(30) NULL,
    detail     NVARCHAR(MAX) NULL,    -- 变更前后关键值(JSON 文本)
    ip         NVARCHAR(45) NULL,
    created_at DATETIME2    NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE INDEX idx_log_user ON operation_log (user_id, created_at);
GO
CREATE INDEX idx_log_doc ON operation_log (doc_type, doc_id);
GO

-- 单据编号序列(行锁取号,作废不回收)
CREATE TABLE doc_sequence (
    id       BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    doc_type NVARCHAR(20) NOT NULL, -- 编号类型:SO/OUT/PIN/RCV/PAY/SKU...
    period   NVARCHAR(10) NOT NULL, -- 期间,如 20260831;不按日期的用 ALL
    next_no  INT          NOT NULL DEFAULT 1 -- 下一个序号
);
GO
CREATE UNIQUE INDEX uk_seq ON doc_sequence (doc_type, period);
GO

-- ------------------------------------------------------------
-- 基础数据
-- ------------------------------------------------------------

-- 商品分类
CREATE TABLE product_category (
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    parent_id  BIGINT       NOT NULL DEFAULT 0, -- 上级分类,0 为根
    name       NVARCHAR(50) NOT NULL,
    sort       INT          NOT NULL DEFAULT 0,
    is_active  TINYINT      NOT NULL DEFAULT 1,
    created_by BIGINT       NULL,
    created_at DATETIME2    NOT NULL DEFAULT SYSDATETIME(),
    updated_by BIGINT       NULL,
    updated_at DATETIME2    NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE INDEX idx_category_parent ON product_category (parent_id);
GO

-- 商品
CREATE TABLE product (
    id             BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code           NVARCHAR(30)   NOT NULL, -- 商品编码,自动生成
    category_id    BIGINT         NOT NULL DEFAULT 0,
    name           NVARCHAR(100)  NOT NULL, -- 名称
    spec           NVARCHAR(100)  NOT NULL DEFAULT N'', -- 规格型号
    unit           NVARCHAR(20)   NOT NULL, -- 基本单位
    barcode        NVARCHAR(50)   NULL,
    purchase_price DECIMAL(18, 2) NOT NULL DEFAULT 0, -- 默认进价
    sale_price     DECIMAL(18, 2) NOT NULL DEFAULT 0, -- 默认售价
    min_sale_price DECIMAL(18, 2) NOT NULL DEFAULT 0, -- 最低限价,订单审核强制校验
    is_active      TINYINT        NOT NULL DEFAULT 1,
    created_by     BIGINT         NULL,
    created_at     DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    updated_by     BIGINT         NULL,
    updated_at     DATETIME2      NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE UNIQUE INDEX uk_product_code ON product (code);
GO
CREATE INDEX idx_product_category ON product (category_id);
GO
CREATE INDEX idx_product_name ON product (name);
GO

-- 客户
CREATE TABLE customer (
    id                BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code              NVARCHAR(30)   NOT NULL, -- 客户编码
    name              NVARCHAR(100)  NOT NULL, -- 名称(唯一)
    short_name        NVARCHAR(50)   NOT NULL DEFAULT N'', -- 简称,搜索用
    contact           NVARCHAR(50)   NOT NULL DEFAULT N'', -- 联系人
    phone             NVARCHAR(20)   NOT NULL DEFAULT N'',
    address           NVARCHAR(200)  NOT NULL DEFAULT N'', -- 默认收货地址
    payment_term_days INT            NOT NULL DEFAULT 0, -- 账期(天),0 = 现结
    credit_limit      DECIMAL(18, 2) NOT NULL DEFAULT 0, -- 信用额度,0 = 不限
    salesperson_id    BIGINT         NOT NULL DEFAULT 0, -- 归属业务员 = 数据权限锚点
    is_active         TINYINT        NOT NULL DEFAULT 1,
    created_by        BIGINT         NULL,
    created_at        DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    updated_by        BIGINT         NULL,
    updated_at        DATETIME2      NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE UNIQUE INDEX uk_customer_code ON customer (code);
GO
CREATE UNIQUE INDEX uk_customer_name ON customer (name);
GO
CREATE INDEX idx_customer_salesperson ON customer (salesperson_id);
GO

-- 供应商
CREATE TABLE supplier (
    id                BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code              NVARCHAR(30)   NOT NULL,
    name              NVARCHAR(100)  NOT NULL,
    contact           NVARCHAR(50)   NOT NULL DEFAULT N'',
    phone             NVARCHAR(20)   NOT NULL DEFAULT N'',
    payment_term_days INT            NOT NULL DEFAULT 0, -- 账期(天)
    settle_type       NVARCHAR(10)   NOT NULL DEFAULT N'现结', -- 结算方式:现结/月结...
    bank_name         NVARCHAR(100)  NOT NULL DEFAULT N'', -- 开户行
    bank_account      NVARCHAR(50)   NOT NULL DEFAULT N'', -- 银行账号
    is_active         TINYINT        NOT NULL DEFAULT 1,
    created_by        BIGINT         NULL,
    created_at        DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    updated_by        BIGINT         NULL,
    updated_at        DATETIME2      NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE UNIQUE INDEX uk_supplier_code ON supplier (code);
GO
CREATE UNIQUE INDEX uk_supplier_name ON supplier (name);
GO

-- 仓库
CREATE TABLE warehouse (
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code       NVARCHAR(30) NOT NULL,
    name       NVARCHAR(50) NOT NULL,
    type       NVARCHAR(10) NOT NULL DEFAULT N'正品仓', -- 正品仓/次品仓/样品仓
    is_active  TINYINT      NOT NULL DEFAULT 1,
    created_by BIGINT       NULL,
    created_at DATETIME2    NOT NULL DEFAULT SYSDATETIME(),
    updated_by BIGINT       NULL,
    updated_at DATETIME2    NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE UNIQUE INDEX uk_warehouse_code ON warehouse (code);
GO

-- ------------------------------------------------------------
-- 库存
-- ------------------------------------------------------------

-- 即时库存(结存)
CREATE TABLE inventory (
    id           BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    product_id   BIGINT         NOT NULL,
    warehouse_id BIGINT         NOT NULL,
    qty          DECIMAL(18, 4) NOT NULL DEFAULT 0, -- 结存数量
    total_cost   DECIMAL(18, 2) NOT NULL DEFAULT 0, -- 结存总成本,加权平均 = total_cost/qty
    version      INT            NOT NULL DEFAULT 0, -- 乐观锁,配合原子扣减
    created_at   DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    updated_at   DATETIME2      NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE UNIQUE INDEX uk_inventory ON inventory (product_id, warehouse_id);
GO

-- 出入库流水(只增不改)
CREATE TABLE inventory_ledger (
    id             BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    doc_type       NVARCHAR(20)   NOT NULL, -- PURCHASE_IN/SALES_OUT/TRANSFER/CHECK_ADJ/OTHER_IN/OTHER_OUT/SALES_RETURN/PURCHASE_RETURN
    doc_id         BIGINT         NOT NULL, -- 来源单据 id,可反查
    doc_no         NVARCHAR(30)   NOT NULL,
    product_id     BIGINT         NOT NULL,
    warehouse_id   BIGINT         NOT NULL,
    direction      TINYINT        NOT NULL, -- 1 入库 -1 出库
    qty            DECIMAL(18, 4) NOT NULL, -- 变动数量,恒为正数
    unit_cost      DECIMAL(18, 2) NOT NULL DEFAULT 0,
    amount         DECIMAL(18, 2) NOT NULL DEFAULT 0,
    balance_qty    DECIMAL(18, 4) NOT NULL, -- 变动后结存数量快照
    balance_amount DECIMAL(18, 2) NOT NULL, -- 变动后结存金额快照
    biz_date       DATE           NOT NULL,
    created_by     BIGINT         NULL,
    created_at     DATETIME2      NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE INDEX idx_ledger_stock ON inventory_ledger (product_id, warehouse_id, biz_date);
GO
CREATE INDEX idx_ledger_doc ON inventory_ledger (doc_type, doc_id);
GO

-- ------------------------------------------------------------
-- 销售
-- ------------------------------------------------------------

-- 销售订单
CREATE TABLE sales_order (
    id             BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    doc_no         NVARCHAR(30)   NOT NULL, -- SOyyyyMMdd-nnnn
    customer_id    BIGINT         NOT NULL,
    salesperson_id BIGINT         NOT NULL, -- 业务员,数据权限
    biz_date       DATE           NOT NULL, -- 业务日期(下单日)
    status         NVARCHAR(10)   NOT NULL DEFAULT N'DRAFT', -- DRAFT/AUDITED/VOID
    ship_status    NVARCHAR(10)   NOT NULL DEFAULT N'UN_SHIPPED', -- UN_SHIPPED/PART_SHIPPED/SHIPPED
    total_amount   DECIMAL(18, 2) NOT NULL DEFAULT 0, -- 总额冗余汇总
    audit_by       BIGINT         NULL,
    audit_at       DATETIME2      NULL,
    remark         NVARCHAR(500)  NOT NULL DEFAULT N'',
    created_by     BIGINT         NULL,
    created_at     DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    updated_by     BIGINT         NULL,
    updated_at     DATETIME2      NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE UNIQUE INDEX uk_so_no ON sales_order (doc_no);
GO
CREATE INDEX idx_so_salesperson ON sales_order (salesperson_id, biz_date);
GO
CREATE INDEX idx_so_customer ON sales_order (customer_id);
GO
CREATE INDEX idx_so_status ON sales_order (status, biz_date);
GO

-- 销售订单明细
CREATE TABLE sales_order_item (
    id          BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    order_id    BIGINT         NOT NULL,
    line_no     INT            NOT NULL, -- 行号,从 1 开始
    product_id  BIGINT         NOT NULL,
    qty         DECIMAL(18, 4) NOT NULL, -- 订货数量
    shipped_qty DECIMAL(18, 4) NOT NULL DEFAULT 0, -- 已发数量(出库审核时累加)
    price       DECIMAL(18, 2) NOT NULL,
    amount      DECIMAL(18, 2) NOT NULL, -- = qty * price,落库冗余
    note        NVARCHAR(200)  NOT NULL DEFAULT N''
);
GO
CREATE INDEX idx_soi_order ON sales_order_item (order_id);
GO

-- 销售出库单
CREATE TABLE sales_outbound (
    id           BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    doc_no       NVARCHAR(30)  NOT NULL, -- OUTyyyyMMdd-nnnn
    order_id     BIGINT        NOT NULL, -- 来源销售订单
    customer_id  BIGINT        NOT NULL, -- 冗余,便于查询
    warehouse_id BIGINT        NOT NULL, -- 发货仓
    biz_date     DATE          NOT NULL,
    status       NVARCHAR(10)  NOT NULL DEFAULT N'DRAFT', -- DRAFT/AUDITED/VOID
    audit_by     BIGINT        NULL,
    audit_at     DATETIME2     NULL,
    remark       NVARCHAR(500) NOT NULL DEFAULT N'',
    created_by   BIGINT        NULL,
    created_at   DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    updated_by   BIGINT        NULL,
    updated_at   DATETIME2     NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE UNIQUE INDEX uk_out_no ON sales_outbound (doc_no);
GO
CREATE INDEX idx_out_order ON sales_outbound (order_id);
GO

-- 销售出库明细
CREATE TABLE sales_outbound_item (
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    outbound_id   BIGINT         NOT NULL,
    order_item_id BIGINT         NOT NULL, -- 对应订单明细行,发货跟踪
    line_no       INT            NOT NULL,
    product_id    BIGINT         NOT NULL,
    warehouse_id  BIGINT         NOT NULL,
    qty           DECIMAL(18, 4) NOT NULL, -- 实发数量,可小于订货数量
    price         DECIMAL(18, 2) NOT NULL, -- 取订单售价,出库不可改价
    amount        DECIMAL(18, 2) NOT NULL,
    cost_price    DECIMAL(18, 2) NOT NULL DEFAULT 0, -- 审核时按加权平均结转的成本单价快照
    cost_amount   DECIMAL(18, 2) NOT NULL DEFAULT 0
);
GO
CREATE INDEX idx_soi_outbound ON sales_outbound_item (outbound_id);
GO

-- 应收账款(出库审核自动生成)
CREATE TABLE receivable (
    id              BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    customer_id     BIGINT         NOT NULL,
    doc_type        NVARCHAR(20)   NOT NULL, -- SALES_OUT / SALES_RETURN
    doc_id          BIGINT         NOT NULL,
    doc_no          NVARCHAR(30)   NOT NULL,
    biz_date        DATE           NOT NULL,
    due_date        DATE           NOT NULL, -- = biz_date + 客户账期
    amount          DECIMAL(18, 2) NOT NULL, -- 应收金额
    received_amount DECIMAL(18, 2) NOT NULL DEFAULT 0, -- 已核销金额
    status          NVARCHAR(10)   NOT NULL DEFAULT N'UNSETTLED', -- UNSETTLED/PARTIAL/SETTLED
    created_at      DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    updated_at      DATETIME2      NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE INDEX idx_recv_customer ON receivable (customer_id, status);
GO
CREATE INDEX idx_recv_due ON receivable (due_date);
GO

-- 收款单
CREATE TABLE receipt (
    id           BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    doc_no       NVARCHAR(30)   NOT NULL, -- RCVyyyyMMdd-nnnn
    customer_id  BIGINT         NOT NULL,
    biz_date     DATE           NOT NULL,
    amount       DECIMAL(18, 2) NOT NULL, -- 收款总额
    method       NVARCHAR(10)   NOT NULL DEFAULT N'转账', -- 转账/现金/承兑...
    bank_account NVARCHAR(50)   NOT NULL DEFAULT N'', -- 收款账户
    status       NVARCHAR(10)   NOT NULL DEFAULT N'DRAFT', -- DRAFT/AUDITED/VOID
    audit_by     BIGINT         NULL,
    audit_at     DATETIME2      NULL,
    remark       NVARCHAR(500)  NOT NULL DEFAULT N'',
    created_by   BIGINT         NULL,
    created_at   DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    updated_by   BIGINT         NULL,
    updated_at   DATETIME2      NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE UNIQUE INDEX uk_rcv_no ON receipt (doc_no);
GO
CREATE INDEX idx_rcv_customer ON receipt (customer_id, biz_date);
GO

-- 收款核销明细;核销合计≤收款额,差额为预收
CREATE TABLE receipt_allocation (
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    receipt_id    BIGINT         NOT NULL,
    receivable_id BIGINT         NOT NULL,
    amount        DECIMAL(18, 2) NOT NULL -- 本次核销金额
);
GO
CREATE INDEX idx_ralloc_receipt ON receipt_allocation (receipt_id);
GO
CREATE INDEX idx_ralloc_receivable ON receipt_allocation (receivable_id);
GO

-- ------------------------------------------------------------
-- 采购(简化版)
-- ------------------------------------------------------------

-- 采购入库单
CREATE TABLE purchase_inbound (
    id           BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    doc_no       NVARCHAR(30)  NOT NULL, -- PINyyyyMMdd-nnnn
    supplier_id  BIGINT        NOT NULL,
    warehouse_id BIGINT        NOT NULL,
    biz_date     DATE          NOT NULL,
    status       NVARCHAR(10)  NOT NULL DEFAULT N'DRAFT', -- DRAFT/AUDITED/VOID
    audit_by     BIGINT        NULL,
    audit_at     DATETIME2     NULL,
    remark       NVARCHAR(500) NOT NULL DEFAULT N'',
    created_by   BIGINT        NULL,
    created_at   DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    updated_by   BIGINT        NULL,
    updated_at   DATETIME2     NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE UNIQUE INDEX uk_pin_no ON purchase_inbound (doc_no);
GO
CREATE INDEX idx_pin_supplier ON purchase_inbound (supplier_id, biz_date);
GO

-- 采购入库明细
CREATE TABLE purchase_inbound_item (
    id           BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    inbound_id   BIGINT         NOT NULL,
    line_no      INT            NOT NULL,
    product_id   BIGINT         NOT NULL,
    warehouse_id BIGINT         NOT NULL,
    qty          DECIMAL(18, 4) NOT NULL,
    price        DECIMAL(18, 2) NOT NULL, -- 进价,即采购成本
    amount       DECIMAL(18, 2) NOT NULL,
    note         NVARCHAR(200)  NOT NULL DEFAULT N''
);
GO
CREATE INDEX idx_pii_inbound ON purchase_inbound_item (inbound_id);
GO

-- 应付账款(入库审核自动生成)
CREATE TABLE payable (
    id          BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    supplier_id BIGINT         NOT NULL,
    doc_type    NVARCHAR(20)   NOT NULL, -- PURCHASE_IN / PURCHASE_RETURN
    doc_id      BIGINT         NOT NULL,
    doc_no      NVARCHAR(30)   NOT NULL,
    biz_date    DATE           NOT NULL,
    due_date    DATE           NOT NULL, -- = biz_date + 供应商账期
    amount      DECIMAL(18, 2) NOT NULL,
    paid_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    status      NVARCHAR(10)   NOT NULL DEFAULT N'UNSETTLED', -- UNSETTLED/PARTIAL/SETTLED
    created_at  DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    updated_at  DATETIME2      NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE INDEX idx_pay_supplier ON payable (supplier_id, status);
GO
CREATE INDEX idx_pay_due ON payable (due_date);
GO

-- 付款单
CREATE TABLE payment (
    id           BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    doc_no       NVARCHAR(30)   NOT NULL, -- PAYyyyyMMdd-nnnn
    supplier_id  BIGINT         NOT NULL,
    biz_date     DATE           NOT NULL,
    amount       DECIMAL(18, 2) NOT NULL,
    method       NVARCHAR(10)   NOT NULL DEFAULT N'转账',
    bank_account NVARCHAR(50)   NOT NULL DEFAULT N'',
    status       NVARCHAR(10)   NOT NULL DEFAULT N'DRAFT', -- DRAFT/AUDITED/VOID
    audit_by     BIGINT         NULL,
    audit_at     DATETIME2      NULL,
    remark       NVARCHAR(500)  NOT NULL DEFAULT N'',
    created_by   BIGINT         NULL,
    created_at   DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    updated_by   BIGINT         NULL,
    updated_at   DATETIME2      NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE UNIQUE INDEX uk_pay_no ON payment (doc_no);
GO
CREATE INDEX idx_payment_supplier ON payment (supplier_id, biz_date);
GO

-- 付款核销明细
CREATE TABLE payment_allocation (
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    payment_id BIGINT         NOT NULL,
    payable_id BIGINT         NOT NULL,
    amount     DECIMAL(18, 2) NOT NULL
);
GO
CREATE INDEX idx_palloc_payment ON payment_allocation (payment_id);
GO
CREATE INDEX idx_palloc_payable ON payment_allocation (payable_id);
GO

-- ------------------------------------------------------------
-- 初始化数据:固定四角色(用户由应用启动器创建)
-- ------------------------------------------------------------

SET IDENTITY_INSERT sys_role ON;

INSERT INTO sys_role (id, code, name, remark) VALUES
    (1, N'ADMIN',     N'管理员',     N'全部权限'),
    (2, N'SALES',     N'销售业务员', N'本人客户与本人单据'),
    (3, N'WAREHOUSE', N'仓管员',     N'出入库与库存'),
    (4, N'FINANCE',   N'财务',       N'收付款与核销');

SET IDENTITY_INSERT sys_role OFF;
GO
