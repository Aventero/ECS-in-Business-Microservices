CREATE TABLE orders
(
    id               VARCHAR(255) PRIMARY KEY,
    cart_id          UUID,
    username         VARCHAR(255)             NOT NULL,
    total_amount     DECIMAL(19, 2)           NOT NULL,
    order_date_time  TIMESTAMP WITH TIME ZONE NOT NULL,
    status           VARCHAR(50)              NOT NULL,
    customer_name    VARCHAR(255)             NOT NULL,
    email            VARCHAR(255)             NOT NULL,
    billing_address  TEXT                     NOT NULL,
    payment_method   VARCHAR(255)             NOT NULL,
    payment_details  TEXT                     NOT NULL,
    shipping_address TEXT                     NOT NULL,
    shipping_method  VARCHAR(255)             NOT NULL
);

CREATE TABLE order_items
(
    id         VARCHAR(255) PRIMARY KEY,
    order_id   VARCHAR(255)   NOT NULL,
    product_id UUID           NOT NULL,
    quantity   INTEGER        NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL
);


CREATE TABLE IF NOT EXISTS carts
(
    cart_id    UUID PRIMARY KEY,
    username   VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cart_items
(
    cart_item_id VARCHAR(50) PRIMARY KEY,
    cart_id      UUID REFERENCES carts (cart_id),
    product_id   UUID,
    price        DECIMAL(10, 2),
    quantity     INTEGER
);