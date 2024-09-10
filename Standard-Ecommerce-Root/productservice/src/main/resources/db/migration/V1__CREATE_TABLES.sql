DROP TABLE IF EXISTS products;

CREATE TABLE IF NOT EXISTS products
(
    id            UUID PRIMARY KEY,
    name          VARCHAR(255)             NOT NULL,
    description   TEXT,
    price         DECIMAL(10, 2)           NOT NULL,
    stock         INTEGER                  NOT NULL,
    category      VARCHAR(255),
    creation_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated  TIMESTAMP WITH TIME ZONE NOT NULL
);