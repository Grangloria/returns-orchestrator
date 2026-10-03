IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'inventory_items')
BEGIN
CREATE TABLE inventory_items (
                                 id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                 sku VARCHAR(100) NOT NULL UNIQUE,
                                 item_name VARCHAR(255) NOT NULL,
                                 quantity INT NOT NULL DEFAULT 10,
                                 updated_at DATETIME2 DEFAULT SYSDATETIME()
);
END;

MERGE INTO inventory_items AS target
    USING (VALUES
               ('SKU-MOUSE-01', 'Wireless Ergonomic Mouse', 10),
               ('SKU-KEYBD-02', 'Mechanical RGB Keyboard', 10),
               ('SKU-MONTR-03', '27-inch 4K Monitor', 10),
               ('SKU-HDSET-04', 'Noise-Canceling Headset', 10),
               ('SKU-DOCK-05', 'USB-C Dual Monitor Dock', 10)
    ) AS source (sku, item_name, quantity)
    ON target.sku = source.sku
    WHEN NOT MATCHED THEN
        INSERT (sku, item_name, quantity)
            VALUES (source.sku, source.item_name, source.quantity);