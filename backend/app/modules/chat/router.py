from typing import List
from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.modules.chat.service import ChatService
from app.modules.chat.schemas import ChannelResponse, MessageResponse, MessageCreate

router = APIRouter(prefix="/chat", tags=["Chat"])

@router.get("/{family_id}/channels", response_model=List[ChannelResponse])
async def get_channels(family_id: str, db: AsyncSession = Depends(get_db)):
    channels = await ChatService.get_or_create_default_channels(db, family_id)
    response = []
    for ch in channels:
        last_msg = ch.messages[-1] if ch.messages else None
        response.append(
            ChannelResponse(
                id=ch.id,
                family_id=ch.family_id,
                name=ch.name,
                channel_type=ch.channel_type,
                is_encrypted=ch.is_encrypted,
                last_message=last_msg.content if last_msg else "No messages yet",
                last_message_time=last_msg.created_at if last_msg else ch.created_at,
                unread_count=0
            )
        )
    return response

@router.get("/{family_id}/channels/{channel_id}/messages", response_model=List[MessageResponse])
async def get_messages(family_id: str, channel_id: str, limit: int = 50, db: AsyncSession = Depends(get_db)):
    return await ChatService.get_messages(db, channel_id, limit)

@router.post("/{family_id}/channels/{channel_id}/messages", response_model=MessageResponse, status_code=status.HTTP_201_CREATED)
async def send_message(family_id: str, channel_id: str, msg: MessageCreate, db: AsyncSession = Depends(get_db)):
    return await ChatService.send_message(db, channel_id, msg)
