import uuid
from datetime import datetime
from sqlalchemy import Column, String, DateTime, ForeignKey, Boolean, Float
from app.core.database import Base

class SafetyZone(Base):
    __tablename__ = "safety_zones"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    family_id = Column(String, ForeignKey("families.id"), nullable=False)
    name = Column(String, nullable=False)
    subtitle = Column(String, nullable=False, default="")
    zone_type = Column(String, nullable=False, default="home") # home, school, work, custom
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)

class SosAlert(Base):
    __tablename__ = "sos_alerts"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    family_id = Column(String, ForeignKey("families.id"), nullable=False)
    sender_name = Column(String, nullable=False)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    status = Column(String, nullable=False, default="ACTIVE") # ACTIVE, RESOLVED
    created_at = Column(DateTime, default=datetime.utcnow)
