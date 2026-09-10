from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.core.database import get_db
from app.modules.safety.models import SafetyZone, SosAlert
from app.modules.safety.schemas import SafetyZoneResponse, SosAlertCreate, SosAlertResponse

router = APIRouter(prefix="/families/{family_id}/safety", tags=["safety"])

@router.get("/zones", response_model=List[SafetyZoneResponse])
async def get_safety_zones(
    family_id: str,
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(
        select(SafetyZone)
        .where(SafetyZone.family_id == family_id)
        .order_by(SafetyZone.created_at.asc())
    )
    zones = result.scalars().all()
    if not zones:
        defaults = [
            SafetyZone(
                family_id=family_id,
                name="Main Residence",
                subtitle="Safe zone • 2 members currently here",
                zone_type="home",
                is_active=True
            ),
            SafetyZone(
                family_id=family_id,
                name="Oakridge School",
                subtitle="Auto check-in & departure notification",
                zone_type="school",
                is_active=True
            ),
            SafetyZone(
                family_id=family_id,
                name="Metro Tech Park",
                subtitle="Work safe zone (Alice & Bob)",
                zone_type="work",
                is_active=True
            )
        ]
        for d in defaults:
            db.add(d)
        await db.commit()
        for d in defaults:
            await db.refresh(d)
        return defaults
    return zones

@router.patch("/zones/{zone_id}/toggle", response_model=SafetyZoneResponse)
async def toggle_safety_zone(
    family_id: str,
    zone_id: str,
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(
        select(SafetyZone).where(SafetyZone.id == zone_id, SafetyZone.family_id == family_id)
    )
    zone = result.scalars().first()
    if not zone:
        raise HTTPException(status_code=404, detail="Zone not found")
    zone.is_active = not zone.is_active
    await db.commit()
    await db.refresh(zone)
    return zone

@router.post("/sos", response_model=SosAlertResponse, status_code=status.HTTP_201_CREATED)
async def broadcast_sos(
    family_id: str,
    payload: SosAlertCreate,
    db: AsyncSession = Depends(get_db)
):
    sos = SosAlert(
        family_id=family_id,
        sender_name=payload.sender_name,
        latitude=payload.latitude,
        longitude=payload.longitude,
        status="ACTIVE"
    )
    db.add(sos)
    await db.commit()
    await db.refresh(sos)
    return sos

@router.get("/sos", response_model=List[SosAlertResponse])
async def get_sos_alerts(
    family_id: str,
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(
        select(SosAlert)
        .where(SosAlert.family_id == family_id)
        .order_by(SosAlert.created_at.desc())
    )
    return result.scalars().all()
