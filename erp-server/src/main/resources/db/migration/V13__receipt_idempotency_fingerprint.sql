-- Preserve a stable request fingerprint so an idempotency key cannot be reused for another payload.
IF COL_LENGTH('receipt', 'idempotency_fingerprint') IS NULL
    ALTER TABLE receipt ADD idempotency_fingerprint NVARCHAR(64) NULL;
GO
