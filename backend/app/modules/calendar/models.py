import uuid
from datetime import datetime
from sqlalchemy import Column, String, DateTime, ForeignKey
from app.core.database import Base

class CalendarEvent(Base):
    __tablename__ = "calendar_events"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    family_id = Column(String, ForeignKey("families.id"), nullable=False)
    title = Column(String, nullable=False)
    start_time = Column(String, nullable=False)
    end_time = Column(String, nullable=True)
    date = Column(String, nullable=False)
    location = Column(String, nullable=False, default="")
    attendee = Column(String, nullable=False, default="All Family")
    category = Column(String, nullable=False, default="Routine")
    created_at = Column(DateTime, default=datetime.utcnow)
