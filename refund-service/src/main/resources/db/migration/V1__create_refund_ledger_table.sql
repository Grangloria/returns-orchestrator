CREATE TABLE refund_ledger (
                               id BIGINT IDENTITY(1,1) PRIMARY KEY,
                               event_id VARCHAR(255) NOT NULL UNIQUE,
                               order_id VARCHAR(255) NOT NULL,
                               amount DECIMAL(18, 2) NOT NULL,
                               status VARCHAR(50) NOT NULL,
                               created_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

CREATE INDEX idx_refund_ledger_event_id ON refund_ledger(event_id);
CREATE INDEX idx_refund_ledger_order_id ON refund_ledger(order_id);