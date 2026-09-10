from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel

class MessageCreate(BaseModel):
    sender_id: str
    sender_name: str
    content: str

class MessageResponse(BaseModel):
    id: str
    channel_id: str
    sender_id: str
    sender_name: str
    content: str
    created_at: datetime

    class Config:
        from_attributes = True

class ChannelCreate(BaseModel):
    name: str
    channel_type: str = "text"
    is_encrypted: bool = True

class ChannelResponse(BaseModel):
    id: str
    family_id: str
    name: str
    channel_type: str
    is_encrypted: bool
    last_message: Optional[str] = None
    last_message_time: Optional[datetime] = None
    unread_count: int = 0

    class Config:
        from_attributes = True
