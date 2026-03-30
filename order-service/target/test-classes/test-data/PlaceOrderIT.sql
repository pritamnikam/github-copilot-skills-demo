-- Minimal seed data for PlaceOrderIT
-- Orders table should be empty before each test (rollback handles this)
-- No seed needed for placeOrder — we're testing creation, not existing data

-- But if you need a pre-existing order for cancelOrder tests:
INSERT INTO orders (id, user_id, product_id, quantity, total_price, status, created_at)
VALUES
  ('ord-existing-1', 'user-1', 'prod-1', 2, 49.99, 'PENDING',  NOW()),
  ('ord-shipped-1',  'user-1', 'prod-2', 1, 29.99, 'SHIPPED',  NOW());