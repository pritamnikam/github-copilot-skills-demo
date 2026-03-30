import pytest
from unittest.mock import MagicMock, AsyncMock
from app.service.inventory_service import InventoryService
from app.repository.inventory_repository import InventoryRepository
from app.client.order_client import OrderClient


# ── Mocks ─────────────────────────────────────────────────────────────────────

@pytest.fixture
def mock_inventory_repo():
    return MagicMock(spec=InventoryRepository)


@pytest.fixture
def mock_order_client():
    return MagicMock(spec=OrderClient)


# ── System under test ─────────────────────────────────────────────────────────

@pytest.fixture
def sut(mock_inventory_repo, mock_order_client):
    return InventoryService(
        repository=mock_inventory_repo,
        order_client=mock_order_client,
    )


# ── Test data factories ───────────────────────────────────────────────────────

@pytest.fixture
def product_in_stock():
    return {
        "product_id": "prod-1",
        "name": "Laptop",
        "stock": 10,
        "reserved": 0,
    }


@pytest.fixture
def product_out_of_stock():
    return {
        "product_id": "prod-2",
        "name": "Sold Out Item",
        "stock": 0,
        "reserved": 0,
    }


@pytest.fixture
def reserve_request():
    return {
        "product_id": "prod-1",
        "quantity": 2,
        "order_id": "ord-123",
    }


@pytest.fixture
def successful_reservation():
    return {
        "reservation_id": "res-abc-123",
        "product_id": "prod-1",
        "quantity": 2,
        "expires_at": "2026-12-01T12:00:00Z",
    }