IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'inventory_items')
BEGIN
CREATE TABLE inventory_items (
                                 item_name VARCHAR(50) NOT NULL PRIMARY KEY,
                                 description VARCHAR(255),
                                 quantity INT NOT NULL
);
END;