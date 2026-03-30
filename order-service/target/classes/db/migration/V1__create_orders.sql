CREATE TABLE orders (
    id          VARCHAR(36)    PRIMARY KEY,
    user_id     VARCHAR(36)    NOT NULL,
    product_id  VARCHAR(36)    NOT NULL,
    quantity    INTEGER        NOT NULL CHECK (quantity > 0),
    total_price NUMERIC(10,2)  NOT NULL,
    status      VARCHAR(20)    NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);