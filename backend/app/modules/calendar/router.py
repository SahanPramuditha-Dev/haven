from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.core.database import get_db
from app.modules.calendar.models import CalendarEvent
from app.modules.calendar.schemas import CalendarEventCreate, CalendarEventResponse

router = APIRouter(prefix="/families/{family_id}/calendar", tags=["calendar"])

@router.get("", response_model=List[CalendarEventResponse])
async def get_family_events(
    family_id: str,
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(
        select(CalendarEvent)
        .where(CalendarEvent.family_id == family_id)
        .order_by(CalendarEvent.created_at.asc())
    )
    events = result.scalars().all()
    if not events:
        # Seed initial routine events for active family
        defaults = [
            CalendarEvent(
                family_id=family_id,
                title="Charlie Soccer Practice",
                start_time="4:30 PM",
                end_time="6:00 PM",
                date="Sunday, Sep 6",
                location="Community Sports Complex",
                attendee="Charlie, Bob",
                category="Sports"
            ),
            CalendarEvent(
                family_id=family_id,
                title="Family Dinner",
                start_time="7:00 PM",
                end_time="8:30 PM",
                date="Sunday, Sep 6",
                location="Dining Room",
                attendee="All Family",
                category="Routine"
            ),
            CalendarEvent(
                family_id=family_id,
                title="Dentist Appointment",
                start_time="10:00 AM",
                end_time="11:00 AM",
                date="Monday, Sep 7",
                location="Central Dental Clinic",
                attendee="Alice",
                category="Health"
            )
        ]
        for d in defaults:
            db.add(d)
        await db.commit()
        for d in defaults:
            await db.refresh(d)
        return defaults
    return events

@router.post("", response_model=CalendarEventResponse, status_code=status.HTTP_201_CREATED)
async def create_event(
    family_id: str,
    payload: CalendarEventCreate,
    db: AsyncSession = Depends(get_db)
):
    event = CalendarEvent(
        family_id=family_id,
        title=payload.title,
        start_time=payload.start_time,
        end_time=payload.end_time,
        date=payload.date,
        location=payload.location,
        attendee=payload.attendee,
        category=payload.category
    )
    db.add(event)
    await db.commit()
    await db.refresh(event)
    return event
