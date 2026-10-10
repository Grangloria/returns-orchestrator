IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'outbox')
BEGIN
CREATE TABLE outbox (
                        id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
                        aggregate_type NVARCHAR(64) NOT NULL,
                        aggregate_id NVARCHAR(64) NOT NULL,
                        type NVARCHAR(64) NOT NULL,
                        payload NVARCHAR(MAX) NOT NULL,
                        status NVARCHAR(32) NOT NULL DEFAULT 'PENDING',
                        created_at DATETIME2 DEFAULT GETDATE()
);

CREATE INDEX idx_outbox_status ON outbox(status);
END;