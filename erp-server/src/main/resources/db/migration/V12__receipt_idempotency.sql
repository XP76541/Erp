-- 客户收款幂等键；空值允许历史单据，非空值全局唯一
IF COL_LENGTH('receipt', 'idempotency_key') IS NULL
    ALTER TABLE receipt ADD idempotency_key NVARCHAR(100) NULL;
GO

IF COL_LENGTH('receipt', 'idempotency_key') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_receipt_idempotency_key' AND object_id = OBJECT_ID('receipt'))
    CREATE UNIQUE INDEX uk_receipt_idempotency_key ON receipt (idempotency_key) WHERE idempotency_key IS NOT NULL;
GO
