from datetime import datetime
from typing import Optional
from pydantic import BaseModel

class TaskCreate(BaseModel):
    title: str
    assigned_to: str

class TaskResponse(BaseModel):
    id: str
    family_id: str
    title: str
    assigned_to: str
    is_completed: bool
    created_at: datetime

    class Config:
        from_attributes = True
