CREATE TABLE outbox (
                        id BIGINT IDENTITY(1,1) PRIMARY KEY,
                        aggregate_type VARCHAR(100) NOT NULL,
                        aggregate_id VARCHAR(100) NOT NULL,
                        event_type VARCHAR(100) NOT NULL,
                        payload NVARCHAR(MAX) NOT NULL,
                        processed BIT DEFAULT 0 NOT NULL,
                        created_at DATETIME2 DEFAULT SYSDATETIME()
);

CREATE INDEX idx_outbox_processed ON outbox(processed);