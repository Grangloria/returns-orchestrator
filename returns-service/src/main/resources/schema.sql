IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[return_manifests]') AND type in (N'U'))

CREATE TABLE dbo.return_manifests (
                                      order_id VARCHAR(50) NOT NULL PRIMARY KEY,
                                      sku VARCHAR(100) NOT NULL,
                                      item_name VARCHAR(255) NOT NULL,
                                      quantity INT NOT NULL,
                                      customer_email VARCHAR(255) NOT NULL,
                                      zip_code VARCHAR(20) NOT NULL,
                                      reason VARCHAR(500) NOT NULL,
                                      status VARCHAR(50) NULL,
                                      label_url VARCHAR(500) NULL,
                                      created_at DATETIME2 DEFAULT GETDATE()
);
