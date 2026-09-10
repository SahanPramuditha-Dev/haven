from typing import List, Optional
from sqlalchemy import select
from sqlalchemy.orm import selectinload
from sqlalchemy.ext.asyncio import AsyncSession
from app.modules.chat.models import Channel, Message
from app.modules.chat.schemas import MessageCreate

class ChatService:
    @staticmethod
    async def get_or_create_default_channels(session: AsyncSession, family_id: str) -> List[Channel]:
        result = await session.execute(
            select(Channel).where(Channel.family_id == family_id).options(selectinload(Channel.messages))
        )
        channels = list(result.scalars().all())
        if not channels:
            default_channels = [
                Channel(family_id=family_id, name="Family General", channel_type="general", is_encrypted=True),
                Channel(family_id=family_id, name="Domestic & Chores", channel_type="chores", is_encrypted=True),
                Channel(family_id=family_id, name="Emergency & Alerts", channel_type="emergency", is_encrypted=True),
                Channel(family_id=family_id, name="Parents Only", channel_type="parents", is_encrypted=True),
            ]
            for ch in default_channels:
                session.add(ch)
            await session.commit()
            result = await session.execute(
                select(Channel).where(Channel.family_id == family_id).options(selectinload(Channel.messages))
            )
            channels = list(result.scalars().all())
        return channels

    @staticmethod
    async def get_messages(session: AsyncSession, channel_id: str, limit: int = 50) -> List[Message]:
        result = await session.execute(
            select(Message).where(Message.channel_id == channel_id).order_by(Message.created_at.asc()).limit(limit)
        )
        return list(result.scalars().all())

    @staticmethod
    async def send_message(session: AsyncSession, channel_id: str, msg_in: MessageCreate) -> Message:
        msg = Message(
            channel_id=channel_id,
            sender_id=msg_in.sender_id,
            sender_name=msg_in.sender_name,
            content=msg_in.content
        )
        session.add(msg)
        await session.commit()
        await session.refresh(msg)
        return msg
