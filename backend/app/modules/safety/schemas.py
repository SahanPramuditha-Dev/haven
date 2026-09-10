from pydantic import BaseModel
from typing import Optional
from datetime import datetime

class SafetyZoneResponse(BaseModel):
    id: str
    family_id: str
    name: str
    subtitle: str
    zone_type: str
    is_active: bool
    created_at: datetime

    class Config:
        from_attributes = True

class SosAlertCreate(BaseModel):
    sender_name: str
    latitude: float
    longitude: float

class SosAlertResponse(BaseModel):
    id: str
    family_id: str
    sender_name: str
    latitude: float
    longitude: float
    status: str
    created_at: datetime

    class Config:
        from_attributes = True
