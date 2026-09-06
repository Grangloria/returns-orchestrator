IF NOT EXISTS (SELECT 1 FROM inventory_items WHERE item_name = 'ITEM-001')
    INSERT INTO inventory_items (item_name, description, quantity) VALUES ('ITEM-001', 'Wireless Ergonomic Mouse', ABS(CHECKSUM(NEWID())) % 191 + 10);

IF NOT EXISTS (SELECT 1 FROM inventory_items WHERE item_name = 'ITEM-002')
    INSERT INTO inventory_items (item_name, description, quantity) VALUES ('ITEM-002', 'Mechanical Gaming Keyboard', ABS(CHECKSUM(NEWID())) % 191 + 10);

IF NOT EXISTS (SELECT 1 FROM inventory_items WHERE item_name = 'ITEM-003')
    INSERT INTO inventory_items (item_name, description, quantity) VALUES ('ITEM-003', '27-Inch 4K UHD Monitor', ABS(CHECKSUM(NEWID())) % 191 + 10);

IF NOT EXISTS (SELECT 1 FROM inventory_items WHERE item_name = 'ITEM-004')
    INSERT INTO inventory_items (item_name, description, quantity) VALUES ('ITEM-004', 'USB-C Dual Docking Station', ABS(CHECKSUM(NEWID())) % 191 + 10);

IF NOT EXISTS (SELECT 1 FROM inventory_items WHERE item_name = 'ITEM-005')
    INSERT INTO inventory_items (item_name, description, quantity) VALUES ('ITEM-005', 'Noise Canceling Headphones', ABS(CHECKSUM(NEWID())) % 191 + 10);

IF NOT EXISTS (SELECT 1 FROM inventory_items WHERE item_name = 'ITEM-006')
    INSERT INTO inventory_items (item_name, description, quantity) VALUES ('ITEM-006', 'HD 1080p Streaming Webcam', ABS(CHECKSUM(NEWID())) % 191 + 10);

IF NOT EXISTS (SELECT 1 FROM inventory_items WHERE item_name = 'ITEM-007')
    INSERT INTO inventory_items (item_name, description, quantity) VALUES ('ITEM-007', 'Standing Desk Converter', ABS(CHECKSUM(NEWID())) % 191 + 10);

IF NOT EXISTS (SELECT 1 FROM inventory_items WHERE item_name = 'ITEM-008')
    INSERT INTO inventory_items (item_name, description, quantity) VALUES ('ITEM-008', 'Ultra-Wide Desk Pad', ABS(CHECKSUM(NEWID())) % 191 + 10);

IF NOT EXISTS (SELECT 1 FROM inventory_items WHERE item_name = 'ITEM-009')
    INSERT INTO inventory_items (item_name, description, quantity) VALUES ('ITEM-009', 'Portable 1TB NVMe SSD', ABS(CHECKSUM(NEWID())) % 191 + 10);

IF NOT EXISTS (SELECT 1 FROM inventory_items WHERE item_name = 'ITEM-010')
    INSERT INTO inventory_items (item_name, description, quantity) VALUES ('ITEM-010', 'Smart Power Strip Surge Protector', ABS(CHECKSUM(NEWID())) % 191 + 10);