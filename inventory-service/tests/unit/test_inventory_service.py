import pytest
from unittest.mock import call
from app.exception.errors import (
    InsufficientStockError,
    ProductNotFoundException,
    InvalidQuantityError,
)


class TestInventoryService:
    """
    Unit tests for InventoryService.
    All external dependencies mocked via conftest.py fixtures.
    No DB, no HTTP, no filesystem.
    """

    # ══════════════════════════════════════════════════════════════════════════
    # reserve_stock
    # ══════════════════════════════════════════════════════════════════════════

    class TestReserveStock:

        def test_happy_path_returns_reservation_and_saves(
            self, sut, mock_inventory_repo, product_in_stock,
            reserve_request, successful_reservation
        ):
            # Arrange
            mock_inventory_repo.find_by_id.return_value = product_in_stock
            mock_inventory_repo.save.return_value = successful_reservation

            # Act
            result = sut.reserve_stock(
                product_id="prod-1", quantity=2, order_id="ord-123"
            )

            # Assert
            assert result["reservation_id"] == "res-abc-123"
            assert result["product_id"] == "prod-1"
            assert result["quantity"] == 2

            # Side effects
            mock_inventory_repo.find_by_id.assert_called_once_with("prod-1")
            mock_inventory_repo.save.assert_called_once()

        # ── Input validation ──────────────────────────────────────────────────

        def test_none_product_id_raises_value_error(self, sut):
            with pytest.raises(ValueError, match="product_id"):
                sut.reserve_stock(
                    product_id=None, quantity=2, order_id="ord-123"
                )

        def test_empty_product_id_raises_value_error(self, sut):
            with pytest.raises(ValueError, match="product_id"):
                sut.reserve_stock(
                    product_id="", quantity=2, order_id="ord-123"
                )

        @pytest.mark.parametrize("qty", [0, -1, -100])
        def test_invalid_quantity_raises_invalid_quantity_error(
            self, sut, qty
        ):
            with pytest.raises(InvalidQuantityError, match="quantity"):
                sut.reserve_stock(
                    product_id="prod-1", quantity=qty, order_id="ord-123"
                )

        def test_none_order_id_raises_value_error(self, sut):
            with pytest.raises(ValueError, match="order_id"):
                sut.reserve_stock(
                    product_id="prod-1", quantity=2, order_id=None
                )

        # ── Dependency failures ───────────────────────────────────────────────

        def test_product_not_found_raises_product_not_found_error(
            self, sut, mock_inventory_repo
        ):
            # Arrange
            mock_inventory_repo.find_by_id.return_value = None

            # Act + Assert
            with pytest.raises(ProductNotFoundException, match="prod-ghost"):
                sut.reserve_stock(
                    product_id="prod-ghost", quantity=2, order_id="ord-123"
                )

            # No save attempted
            mock_inventory_repo.save.assert_not_called()

        def test_insufficient_stock_raises_error(
            self, sut, mock_inventory_repo, product_in_stock
        ):
            # Arrange — only 10 in stock
            mock_inventory_repo.find_by_id.return_value = product_in_stock

            # Act + Assert — requesting 999
            with pytest.raises(InsufficientStockError) as exc_info:
                sut.reserve_stock(
                    product_id="prod-1", quantity=999, order_id="ord-123"
                )

            assert exc_info.value.available == 10
            mock_inventory_repo.save.assert_not_called()

        def test_out_of_stock_product_raises_insufficient_stock_error(
            self, sut, mock_inventory_repo, product_out_of_stock
        ):
            mock_inventory_repo.find_by_id.return_value = product_out_of_stock

            with pytest.raises(InsufficientStockError):
                sut.reserve_stock(
                    product_id="prod-2", quantity=1, order_id="ord-123"
                )

        def test_repo_save_failure_propagates_exception(
            self, sut, mock_inventory_repo, product_in_stock
        ):
            # Arrange
            mock_inventory_repo.find_by_id.return_value = product_in_stock
            mock_inventory_repo.save.side_effect = RuntimeError("DB timeout")

            # Act + Assert
            with pytest.raises(RuntimeError, match="DB timeout"):
                sut.reserve_stock(
                    product_id="prod-1", quantity=2, order_id="ord-123"
                )

        def test_reservation_saved_with_correct_data(
            self, sut, mock_inventory_repo, product_in_stock,
            successful_reservation
        ):
            # Arrange
            mock_inventory_repo.find_by_id.return_value = product_in_stock
            mock_inventory_repo.save.return_value = successful_reservation

            # Act
            sut.reserve_stock(
                product_id="prod-1", quantity=2, order_id="ord-123"
            )

            # Assert save called with correct reservation data
            saved = mock_inventory_repo.save.call_args[0][0]
            assert saved["product_id"] == "prod-1"
            assert saved["quantity"] == 2
            assert saved["order_id"] == "ord-123"

    # ══════════════════════════════════════════════════════════════════════════
    # release_reservation
    # ══════════════════════════════════════════════════════════════════════════

    class TestReleaseReservation:

        def test_happy_path_deletes_and_returns_true(
            self, sut, mock_inventory_repo
        ):
            # Arrange
            mock_inventory_repo.find_reservation_by_id.return_value = {
                "reservation_id": "res-abc", "product_id": "prod-1"
            }

            # Act
            result = sut.release_reservation("res-abc")

            # Assert
            assert result is True
            mock_inventory_repo.delete_reservation.assert_called_once_with(
                "res-abc"
            )

        def test_none_reservation_id_raises_value_error(self, sut):
            with pytest.raises(ValueError, match="reservation_id"):
                sut.release_reservation(None)

        def test_reservation_not_found_raises_error(
            self, sut, mock_inventory_repo
        ):
            from app.exception.errors import ReservationNotFoundError
            mock_inventory_repo.find_reservation_by_id.return_value = None

            with pytest.raises(ReservationNotFoundError, match="res-ghost"):
                sut.release_reservation("res-ghost")

            mock_inventory_repo.delete_reservation.assert_not_called()