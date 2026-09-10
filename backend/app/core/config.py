import sys
from typing import List, Union
from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    PROJECT_NAME: str = "HAVEN Family OS API"
    VERSION: str = "1.0.0"
    ENVIRONMENT: str = "development" # development | staging | production
    DEBUG: bool = False

    # Security
    SECRET_KEY: str = "dev-insecure-secret-key-replace-in-prod"
    ALLOWED_ORIGINS: Union[str, List[str]] = "*"

    # Neon PostgreSQL Pooled Endpoint
    # Format: postgresql+asyncpg://user:pass@ep-pooler-xyz.region.neon.tech/haven_db?ssl=require
    DATABASE_URL: str = "postgresql+asyncpg://postgres:postgres@localhost:5432/haven_db"

    # Firebase Admin
    FIREBASE_PROJECT_ID: str = "haven-family-os"
    FIREBASE_CREDENTIALS_JSON: str = "" # Injected as secret string or file path
    FIREBASE_CREDENTIALS_PATH: str = ""

    # Cloudflare R2
    R2_ACCOUNT_ID: str = ""
    R2_ACCESS_KEY_ID: str = ""
    R2_SECRET_ACCESS_KEY: str = ""
    R2_BUCKET_NAME: str = "haven-media"
    R2_PUBLIC_DOMAIN: str = ""

    model_config = SettingsConfigDict(
        env_file=(".env", "backend/.env"),
        env_file_encoding="utf-8",
        case_sensitive=True,
        extra="ignore"
    )

    @field_validator("ALLOWED_ORIGINS", mode="before")
    @classmethod
    def assemble_cors_origins(cls, v: Union[str, List[str]]) -> List[str]:
        if isinstance(v, str) and not v.startswith("["):
            return [i.strip() for i in v.split(",")]
        elif isinstance(v, list):
            return v
        return ["*"]

    def validate_production(self):
        if self.ENVIRONMENT == "production":
            if not self.DATABASE_URL or "localhost" in self.DATABASE_URL or "127.0.0.1" in self.DATABASE_URL:
                print("FATAL: Production DATABASE_URL must point to remote Neon PostgreSQL", file=sys.stderr)
                sys.exit(1)
            if not self.FIREBASE_PROJECT_ID:
                print("FATAL: Production requires valid FIREBASE_PROJECT_ID", file=sys.stderr)
                sys.exit(1)

settings = Settings()
settings.validate_production()
