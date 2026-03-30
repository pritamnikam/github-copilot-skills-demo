# Inventory Service — Unit Test Standards

## Stack
Language: Python 3.12
Framework: FastAPI
Testing: pytest + unittest.mock

## System under test naming
Always name the fixture: sut
Always import from: tests/conftest.py

## Mocks for InventoryService
mock_inventory_repo  → MagicMock(spec=InventoryRepository)
mock_order_client    → MagicMock(spec=OrderClient)

## Exception types
InsufficientStockError   → quantity requested > available stock
ProductNotFoundException → product_id not in database
ReservationExpiredError  → reservation TTL exceeded
InvalidQuantityError     → quantity <= 0

## Mandatory scenarios for reserve_stock(product_id, quantity, order_id)
  [ ] happy path → returns reservation dict, repo.save called
  [ ] product_id is None → raises ValueError
  [ ] product_id is empty string → raises ValueError
  [ ] quantity is 0 → raises InvalidQuantityError
  [ ] quantity is negative → raises InvalidQuantityError
  [ ] product not found → raises ProductNotFoundException
  [ ] insufficient stock → raises InsufficientStockError
  [ ] verify: repo.save called with correct reservation data
  [ ] verify: repo.save NOT called when product not found

## Mandatory scenarios for release_reservation(reservation_id)
  [ ] happy path → reservation deleted, returns True
  [ ] reservation_id is None → raises ValueError
  [ ] reservation not found → raises ReservationNotFoundError
  [ ] verify: repo.delete called exactly once

## Coverage target
  inventory_service.py: 90% line coverage
  reservation_service.py: 88% line coverage
  Run with: pytest tests/unit/ --cov=app --cov-report=term-missing