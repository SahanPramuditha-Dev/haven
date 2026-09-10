from typing import List
from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.core.security import get_current_user_token, AuthenticatedUser
from app.modules.families.schemas import (
    CreateFamilyRequest,
    JoinFamilyRequest,
    FamilyResponse,
    FamilyMemberResponse,
    UserResponse
)
from app.modules.families.service import FamilyService

router = APIRouter(prefix="/families", tags=["Families"])

@router.get("/me", response_model=UserResponse)
async def get_my_profile(
    auth_user: AuthenticatedUser = Depends(get_current_user_token),
    session: AsyncSession = Depends(get_db)
):
    user = await FamilyService.get_or_create_user(session, auth_user)
    return user

@router.post("", response_model=FamilyResponse, status_code=status.HTTP_201_CREATED)
async def create_family(
    request: CreateFamilyRequest,
    auth_user: AuthenticatedUser = Depends(get_current_user_token),
    session: AsyncSession = Depends(get_db)
):
    user = await FamilyService.get_or_create_user(session, auth_user)
    return await FamilyService.create_family(
        session=session,
        user=user,
        family_name=request.family_name,
        creator_name=request.my_display_name,
        household_name=request.primary_household_name or "Main Home"
    )

@router.post("/join", response_model=FamilyMemberResponse, status_code=status.HTTP_200_OK)
async def join_family(
    request: JoinFamilyRequest,
    auth_user: AuthenticatedUser = Depends(get_current_user_token),
    session: AsyncSession = Depends(get_db)
):
    user = await FamilyService.get_or_create_user(session, auth_user)
    return await FamilyService.join_family(
        session=session,
        user=user,
        invitation_code=request.invitation_code,
        display_name=request.my_display_name
    )

@router.get("", response_model=List[FamilyResponse])
async def list_my_families(
    auth_user: AuthenticatedUser = Depends(get_current_user_token),
    session: AsyncSession = Depends(get_db)
):
    user = await FamilyService.get_or_create_user(session, auth_user)
    return await FamilyService.get_user_families(session, user.id)
