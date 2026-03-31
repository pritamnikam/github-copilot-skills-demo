INSERT INTO orders (id, user_id, product_id, quantity,
                    total_price, status, created_at)
VALUES
  ('ord-pending-1', 'user-1', 'prod-1', 2,
   49.99, 'PENDING',  NOW()),
  ('ord-shipped-1', 'user-1', 'prod-2', 1,
   29.99, 'SHIPPED',  NOW());