import uuid
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload
from app.core.database import get_db
from app.core.security import get_current_user_token, AuthenticatedUser
from app.modules.families.models import User, FamilyMember, Family
from app.modules.auth.schemas import RegisterRequest, LoginRequest, AuthTokenResponse, UserProfileResponse
from app.modules.auth.security_jwt import get_password_hash, verify_password, create_access_token

router = APIRouter(prefix="/auth", tags=["Authentication"])

@router.post("/register", response_model=AuthTokenResponse, status_code=status.HTTP_201_CREATED)
async def register_user(payload: RegisterRequest, db: AsyncSession = Depends(get_db)):
    # Check if user already exists
    existing = await db.execute(select(User).where(User.email == payload.email.lower()))
    if existing.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="An account with this email already exists"
        )

    user_id = str(uuid.uuid4())
    user = User(
        id=user_id,
        firebase_uid=f"local_{user_id}",
        email=payload.email.lower(),
        display_name=payload.display_name,
        hashed_password=get_password_hash(payload.password)
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)

    token = create_access_token({"sub": str(user.id), "email": user.email, "name": user.display_name})
    return AuthTokenResponse(
        access_token=token,
        user_id=str(user.id),
        email=user.email,
        display_name=user.display_name
    )

@router.post("/login", response_model=AuthTokenResponse)
async def login_user(payload: LoginRequest, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(User)
        .options(selectinload(User.family_members).selectinload(FamilyMember.family))
        .where(User.email == payload.email.lower())
    )
    user = result.scalar_one_or_none()
    if not user or not user.hashed_password or not verify_password(payload.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password"
        )

    active_member = user.family_members[0] if user.family_members else None
    family_id = str(active_member.family.id) if active_member and active_member.family else None
    family_name = active_member.family.name if active_member and active_member.family else None

    token = create_access_token({"sub": str(user.id), "email": user.email, "name": user.display_name or ""})
    return AuthTokenResponse(
        access_token=token,
        user_id=str(user.id),
        email=user.email,
        display_name=user.display_name or "User",
        family_id=family_id,
        family_name=family_name
    )

@router.get("/me", response_model=UserProfileResponse)
async def get_current_user_profile(
    auth_user: AuthenticatedUser = Depends(get_current_user_token),
    db: AsyncSession = Depends(get_db)
):
    # Lookup user by Firebase UID or internal UUID string
    result = await db.execute(
        select(User)
        .options(selectinload(User.family_members).selectinload(FamilyMember.family))
        .where((User.id == auth_user.firebase_uid) | (User.firebase_uid == auth_user.firebase_uid))
    )
    user = result.scalar_one_or_none()

    # If user authenticated with Firebase but doesn't exist yet in HAVEN DB, provision internal UUID record
    if not user:
        user_id = str(uuid.uuid4())
        user = User(
            id=user_id,
            firebase_uid=auth_user.firebase_uid,
            email=auth_user.email,
            display_name=auth_user.email.split("@")[0] if auth_user.email else "User",
        )
        db.add(user)
        await db.commit()
        # Re-fetch with family_members loaded so AsyncSession doesn't fail on lazy load
        result = await db.execute(
            select(User)
            .options(selectinload(User.family_members).selectinload(FamilyMember.family))
            .where(User.id == user.id)
        )
        user = result.scalar_one()

    active_member = user.family_members[0] if user.family_members else None
    family_id = str(active_member.family.id) if active_member and active_member.family else None
    family_name = active_member.family.name if active_member and active_member.family else None

    return UserProfileResponse(
        id=str(user.id),
        email=user.email or "",
        display_name=user.display_name or "User",
        family_id=family_id,
        family_name=family_name
    )

