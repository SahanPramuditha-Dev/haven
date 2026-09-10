from typing import Optional, List
from datetime import datetime
import uuid
from pydantic import BaseModel, ConfigDict

class UserResponse(BaseModel):
    id: uuid.UUID
    firebase_uid: str
    email: Optional[str] = None
    phone: Optional[str] = None
    created_at: datetime
    model_config = ConfigDict(from_attributes=True)

class FamilyMemberResponse(BaseModel):
    id: uuid.UUID
    family_id: uuid.UUID
    user_id: Optional[uuid.UUID] = None
    display_name: str
    role: str
    avatar_url: Optional[str] = None
    color_code: Optional[str] = None
    is_active: bool
    model_config = ConfigDict(from_attributes=True)

class HouseholdResponse(BaseModel):
    id: uuid.UUID
    family_id: uuid.UUID
    name: str
    address: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    radius_meters: int
    model_config = ConfigDict(from_attributes=True)

class FamilyResponse(BaseModel):
    id: uuid.UUID
    name: str
    invitation_code: str
    created_by: uuid.UUID
    created_at: datetime
    members: List[FamilyMemberResponse] = []
    households: List[HouseholdResponse] = []
    model_config = ConfigDict(from_attributes=True)

class CreateFamilyRequest(BaseModel):
    family_name: str
    my_display_name: str
    primary_household_name: Optional[str] = "Main Home"

class JoinFamilyRequest(BaseModel):
    invitation_code: str
    my_display_name: str

class AddMemberRequest(BaseModel):
    display_name: str
    role: str = "PARENT_GUARDIAN"
    color_code: Optional[str] = "#4E6058"
