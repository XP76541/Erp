-- 对齐应收账款实体与一期业务字段，兼容收款核销及账龄查询
IF COL_LENGTH('receivable', 'order_id') IS NULL
    ALTER TABLE receivable ADD order_id BIGINT NULL;
IF COL_LENGTH('receivable', 'order_doc_no') IS NULL
    ALTER TABLE receivable ADD order_doc_no NVARCHAR(30) NULL;
IF COL_LENGTH('receivable', 'customer_name') IS NULL
    ALTER TABLE receivable ADD customer_name NVARCHAR(100) NULL;
IF COL_LENGTH('receivable', 'remaining_amount') IS NULL
    ALTER TABLE receivable ADD remaining_amount DECIMAL(18,2) NOT NULL CONSTRAINT df_receivable_remaining DEFAULT 0;
IF COL_LENGTH('receivable', 'days_overdue') IS NULL
    ALTER TABLE receivable ADD days_overdue INT NULL;
IF COL_LENGTH('receivable', 'aging_bucket') IS NULL
    ALTER TABLE receivable ADD aging_bucket NVARCHAR(20) NULL;
GO
-- 历史数据初始剩余金额按应收金额减已核销金额计算
UPDATE receivable
SET remaining_amount = amount - received_amount
WHERE remaining_amount = 0 AND (amount - received_amount) <> 0;
GO
