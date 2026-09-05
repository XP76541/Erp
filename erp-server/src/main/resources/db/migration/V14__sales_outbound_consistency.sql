-- 第三阶段：销售出库与应收来源幂等及明细字段约束
IF COL_LENGTH('sales_outbound', 'total_amount') IS NULL
BEGIN
    ALTER TABLE sales_outbound ADD total_amount DECIMAL(18, 2) NOT NULL CONSTRAINT df_sales_out_total_amount DEFAULT 0;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_soi_outbound_line' AND object_id = OBJECT_ID('sales_outbound_item'))
BEGIN
    CREATE UNIQUE INDEX uk_soi_outbound_line ON sales_outbound_item (outbound_id, line_no);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_soi_outbound_order_item' AND object_id = OBJECT_ID('sales_outbound_item'))
BEGIN
    CREATE UNIQUE INDEX uk_soi_outbound_order_item ON sales_outbound_item (outbound_id, order_item_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_receivable_source' AND object_id = OBJECT_ID('receivable'))
BEGIN
    CREATE UNIQUE INDEX uk_receivable_source ON receivable (doc_type, doc_id);
END
GO
