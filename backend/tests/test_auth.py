import pytest
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from app.main import app
from app.core.database import engine, Base

@pytest_asyncio.fixture(autouse=True)
async def prepare_db():
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
        await conn.run_sync(Base.metadata.create_all)
    yield

@pytest.mark.asyncio
async def test_auth_register_and_login_flow():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url='http://test') as client:
        # 1. Register new user with valid public domain
        reg_response = await client.post(
            '/api/v1/auth/register',
            json={
                'email': 'testparent@havenapp.io',
                'password': 'SecurePassword123!',
                'display_name': 'Test Parent'
            }
        )
        assert reg_response.status_code == 201, reg_response.text
        reg_data = reg_response.json()
        assert 'access_token' in reg_data
        assert reg_data['email'] == 'testparent@havenapp.io'
        assert reg_data['display_name'] == 'Test Parent'
        token = reg_data['access_token']

        # 2. Duplicate registration fails
        dup_response = await client.post(
            '/api/v1/auth/register',
            json={
                'email': 'testparent@havenapp.io',
                'password': 'AnotherPassword!',
                'display_name': 'Clone'
            }
        )
        assert dup_response.status_code == 400

        # 3. Login with correct credentials
        login_response = await client.post(
            '/api/v1/auth/login',
            json={
                'email': 'testparent@havenapp.io',
                'password': 'SecurePassword123!'
            }
        )
        assert login_response.status_code == 200, login_response.text
        login_data = login_response.json()
        assert 'access_token' in login_data
        login_token = login_data['access_token']

        # 4. Login with incorrect password fails
        bad_login = await client.post(
            '/api/v1/auth/login',
            json={
                'email': 'testparent@havenapp.io',
                'password': 'WrongPassword!'
            }
        )
        assert bad_login.status_code == 401

        # 5. Fetch /me with token
        me_response = await client.get(
            '/api/v1/auth/me',
            headers={'Authorization': f'Bearer {login_token}'}
        )
        assert me_response.status_code == 200, me_response.text
        me_data = me_response.json()
        assert me_data['email'] == 'testparent@havenapp.io'
        assert me_data['display_name'] == 'Test Parent'
