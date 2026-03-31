import pytest
from jsonschema import validate
from .conftest import load_schema


@pytest.mark.asyncio
class TestReserveStockApi:
    """
    API/functional tests for POST /api/inventory/reserve
    Tests HTTP boundary: status codes, schema, auth, errors.
    No business logic assertions — those belong in unit tests.
    """

    ENDPOINT = "/api/inventory/reserve"

    VALID_BODY = {
        "productId": "prod-1",
        "quantity":  2,
        "orderId":   "ord-123",
    }

    # ══════════════════════════════════════════════════════════════════════════
    # 2xx — success
    # ══════════════════════════════════════════════════════════════════════════

    async def test_valid_request_returns_200(self, auth_client, db_with_stock):
        response = await auth_client.post(
            self.ENDPOINT, json=self.VALID_BODY
        )
        assert response.status_code == 200

    async def test_valid_request_content_type_is_json(
        self, auth_client, db_with_stock
    ):
        response = await auth_client.post(
            self.ENDPOINT, json=self.VALID_BODY
        )
        assert "application/json" in response.headers["content-type"]

    async def test_valid_request_response_matches_schema(
        self, auth_client, db_with_stock
    ):
        response = await auth_client.post(
            self.ENDPOINT, json=self.VALID_BODY
        )
        assert response.status_code == 200
        validate(
            instance=response.json(),
            schema=load_schema("reservation-response.json")
        )

    async def test_valid_request_returns_correct_product_and_quantity(
        self, auth_client, db_with_stock
    ):
        response = await auth_client.post(
            self.ENDPOINT, json=self.VALID_BODY
        )
        body = response.json()
        assert body["productId"] == "prod-1"
        assert body["quantity"]  == 2
        assert body["reservationId"] is not None
        assert body["expiresAt"]     is not None

    async def test_minimum_quantity_one_accepted(
        self, auth_client, db_with_stock
    ):
        body = {**self.VALID_BODY, "quantity": 1}
        response = await auth_client.post(self.ENDPOINT, json=body)
        assert response.status_code == 200

    # ══════════════════════════════════════════════════════════════════════════
    # 400 — validation errors
    # ══════════════════════════════════════════════════════════════════════════

    async def test_missing_product_id_returns_400(self, auth_client):
        body = {"quantity": 2, "orderId": "ord-123"}
        response = await auth_client.post(self.ENDPOINT, json=body)
        assert response.status_code == 422  # FastAPI validation
        assert "productId" in str(response.json())

    async def test_quantity_zero_returns_422(self, auth_client):
        body = {**self.VALID_BODY, "quantity": 0}
        response = await auth_client.post(self.ENDPOINT, json=body)
        assert response.status_code == 422

    async def test_quantity_negative_returns_422(self, auth_client):
        body = {**self.VALID_BODY, "quantity": -1}
        response = await auth_client.post(self.ENDPOINT, json=body)
        assert response.status_code == 422

    async def test_empty_body_returns_422(self, auth_client):
        response = await auth_client.post(self.ENDPOINT, json={})
        assert response.status_code == 422

    async def test_wrong_content_type_returns_415(self, auth_client):
        response = await auth_client.post(
            self.ENDPOINT,
            content="productId=prod-1&quantity=2",
            headers={"Content-Type": "application/x-www-form-urlencoded"}
        )
        assert response.status_code in (415, 422)

    async def test_get_method_not_allowed_returns_405(self, auth_client):
        response = await auth_client.get(self.ENDPOINT)
        assert response.status_code == 405

    # ══════════════════════════════════════════════════════════════════════════
    # 401 — auth errors
    # ══════════════════════════════════════════════════════════════════════════

    async def test_no_token_returns_401(self, client):
        response = await client.post(self.ENDPOINT, json=self.VALID_BODY)
        assert response.status_code == 401
        body = response.json()
        assert body["code"] == "UNAUTHORIZED"
        validate(instance=body, schema=load_schema("error-response.json"))

    async def test_expired_token_returns_401(self, expired_client):
        response = await expired_client.post(
            self.ENDPOINT, json=self.VALID_BODY
        )
        assert response.status_code == 401
        assert response.json()["code"] == "TOKEN_EXPIRED"

    async def test_malformed_token_returns_401(self, client):
        response = await client.post(
            self.ENDPOINT,
            json=self.VALID_BODY,
            headers={"Authorization": "Bearer not.a.real.jwt"}
        )
        assert response.status_code == 401

    # ══════════════════════════════════════════════════════════════════════════
    # 404 / 409 — business errors
    # ══════════════════════════════════════════════════════════════════════════

    async def test_product_not_found_returns_404(
        self, auth_client, db_with_stock
    ):
        body = {**self.VALID_BODY, "productId": "prod-ghost"}
        response = await auth_client.post(self.ENDPOINT, json=body)
        assert response.status_code == 404
        body_json = response.json()
        assert body_json["code"] == "PRODUCT_NOT_FOUND"
        validate(instance=body_json, schema=load_schema("error-response.json"))

    async def test_insufficient_stock_returns_409(
        self, auth_client, db_with_stock
    ):
        body = {**self.VALID_BODY, "quantity": 9999}
        response = await auth_client.post(self.ENDPOINT, json=body)
        assert response.status_code == 409
        body_json = response.json()
        assert body_json["code"] == "INSUFFICIENT_STOCK"
        assert "available" in body_json

    async def test_out_of_stock_product_returns_409(
        self, auth_client, db_out_of_stock
    ):
        response = await auth_client.post(
            self.ENDPOINT,
            json={**self.VALID_BODY, "productId": "prod-empty"}
        )
        assert response.status_code == 409
        assert response.json()["code"] == "INSUFFICIENT_STOCK"