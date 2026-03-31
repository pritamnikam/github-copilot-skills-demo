import pytest
import jwt
from datetime import datetime, timedelta, timezone
from httpx import AsyncClient
from app.main import app

# Must match app config
TEST_JWT_SECRET = "test-secret-key-minimum-32-chars"
TEST_ALGORITHM  = "HS256"


# ── Token helpers ─────────────────────────────────────────────────────────────

def _make_token(user_id: str, role: str, expires_delta: timedelta) -> str:
    payload = {
        "sub":   user_id,
        "role":  role,
        "iat":   datetime.now(timezone.utc),
        "exp":   datetime.now(timezone.utc) + expires_delta,
    }
    return jwt.encode(payload, TEST_JWT_SECRET, algorithm=TEST_ALGORITHM)


@pytest.fixture
def valid_token():
    return _make_token("user-1", "ROLE_USER", timedelta(hours=1))


@pytest.fixture
def expired_token():
    return _make_token("user-1", "ROLE_USER", timedelta(hours=-1))


@pytest.fixture
def admin_token():
    return _make_token("admin-1", "ROLE_ADMIN", timedelta(hours=1))


# ── HTTP client fixtures ──────────────────────────────────────────────────────

@pytest.fixture
async def client():
    """Unauthenticated httpx client — use for 401 tests."""
    async with AsyncClient(app=app, base_url="http://test") as c:
        yield c


@pytest.fixture
async def auth_client(valid_token):
    """Authenticated httpx client — use for all protected endpoint tests."""
    headers = {"Authorization": f"Bearer {valid_token}"}
    async with AsyncClient(
        app=app, base_url="http://test", headers=headers
    ) as c:
        yield c


@pytest.fixture
async def expired_client(expired_token):
    headers = {"Authorization": f"Bearer {expired_token}"}
    async with AsyncClient(
        app=app, base_url="http://test", headers=headers
    ) as c:
        yield c


# ── Schema loader ─────────────────────────────────────────────────────────────

import json
from pathlib import Path


def load_schema(schema_name: str) -> dict:
    schema_path = Path(__file__).parent.parent / "schemas" / schema_name
    return json.loads(schema_path.read_text())