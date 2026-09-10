from pydantic import BaseModel
from typing import Optional
from datetime import datetime

class CalendarEventCreate(BaseModel):
    title: str
    start_time: str
    end_time: Optional[str] = None
    date: str
    location: str = ""
    attendee: str = "All Family"
    category: str = "Routine"

class CalendarEventResponse(BaseModel):
    id: str
    family_id: str
    title: str
    start_time: str
    end_time: Optional[str] = None
    date: str
    location: str
    attendee: str
    category: str
    created_at: datetime

    class Config:
        from_attributes = True
