DROP TABLE IF EXISTS return_manifests;

CREATE TABLE return_manifests (
                                  order_id VARCHAR(50) NOT NULL PRIMARY KEY,
                                  sku VARCHAR(100) NOT NULL,
                                  item_name VARCHAR(255) NOT NULL,
                                  quantity INT NOT NULL,
                                  customer_email VARCHAR(255) NOT NULL,
                                  zip_code VARCHAR(20) NOT NULL,
                                  reason VARCHAR(500) NOT NULL,
                                  status VARCHAR(50),
                                  label_url VARCHAR(500),
                                  created_at DATETIME2 DEFAULT GETDATE()
);