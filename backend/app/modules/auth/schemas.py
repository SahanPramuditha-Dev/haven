import uuid
from typing import Optional
from pydantic import BaseModel, EmailStr, Field

class RegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=6)
    display_name: str = Field(..., min_length=1)

class LoginRequest(BaseModel):
    email: EmailStr
    password: str

class AuthTokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id: str
    email: str
    display_name: str
    family_id: Optional[str] = None
    family_name: Optional[str] = None

class UserProfileResponse(BaseModel):
    id: str
    email: str
    display_name: str
    family_id: Optional[str] = None
    family_name: Optional[str] = None
