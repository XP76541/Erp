-- 对账单调整记录；调整作为独立业务流水参与客户对账单计算
IF OBJECT_ID('statement_adjustment', 'U') IS NULL
BEGIN
    CREATE TABLE statement_adjustment (
        id                BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        statement_id      BIGINT NULL,
        customer_id       BIGINT NOT NULL,
        customer_name     NVARCHAR(100) NOT NULL DEFAULT N'',
        adjustment_date   DATE NOT NULL,
        adjustment_amount DECIMAL(18,2) NOT NULL,
        adjustment_type   NVARCHAR(30) NOT NULL,
        reason            NVARCHAR(500) NOT NULL DEFAULT N'',
        remark            NVARCHAR(500) NULL,
        operator          NVARCHAR(30) NULL,
        operator_name     NVARCHAR(50) NULL,
        created_at        DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        updated_at        DATETIME2 NULL
    );
    CREATE INDEX idx_statement_adjustment_customer_date
        ON statement_adjustment (customer_id, adjustment_date);
    CREATE INDEX idx_statement_adjustment_statement
        ON statement_adjustment (statement_id);
END;
GO
