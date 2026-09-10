import pytest
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from app.main import app
from app.core.database import Base, get_db

# Use in-memory SQLite for super-fast, isolated unit testing without external services
TEST_DATABASE_URL = "sqlite+aiosqlite:///:memory:"

@pytest_asyncio.fixture(scope="session")
async def test_engine():
    engine = create_async_engine(TEST_DATABASE_URL, echo=False)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield engine
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
    await engine.dispose()

@pytest_asyncio.fixture
async def db_session(test_engine):
    async_session = async_sessionmaker(test_engine, class_=AsyncSession, expire_on_commit=False)
    async with async_session() as session:
        yield session

@pytest_asyncio.fixture
async def client(test_engine):
    async_session = async_sessionmaker(test_engine, class_=AsyncSession, expire_on_commit=False)
    async def override_get_db():
        async with async_session() as session:
            yield session

    app.dependency_overrides[get_db] = override_get_db
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac
    app.dependency_overrides.clear()

@pytest.mark.asyncio
async def test_health(client):
    response = await client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "healthy"

@pytest.mark.asyncio
async def test_unauthorized_family_creation(client):
    response = await client.post("/api/v1/families", json={
        "family_name": "Test Family",
        "my_display_name": "Alice"
    })
    assert response.status_code == 401

@pytest.mark.asyncio
async def test_authorized_family_creation(client):
    headers = {"Authorization": "Bearer test_token:user_123:alice@haven.family"}
    response = await client.post("/api/v1/families", headers=headers, json={
        "family_name": "Haven Family",
        "my_display_name": "Alice",
        "primary_household_name": "Main Cottage"
    })
    assert response.status_code == 201
    data = response.json()
    assert data["name"] == "Haven Family"
    assert len(data["members"]) == 1
    assert data["members"][0]["display_name"] == "Alice"
    assert data["members"][0]["role"] == "FAMILY_ADMIN"
    assert len(data["households"]) == 1
    assert data["households"][0]["name"] == "Main Cottage"
    invite_code = data["invitation_code"]

    # Bob joins Alice's family
    bob_headers = {"Authorization": "Bearer test_token:user_456:bob@haven.family"}
    join_resp = await client.post("/api/v1/families/join", headers=bob_headers, json={
        "invitation_code": invite_code,
        "my_display_name": "Bob"
    })
    assert join_resp.status_code == 200
    bob_member = join_resp.json()
    assert bob_member["display_name"] == "Bob"
    assert bob_member["role"] == "ADULT"
