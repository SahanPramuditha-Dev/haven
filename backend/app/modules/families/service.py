import secrets
import string
import uuid
from typing import Optional, List
from sqlalchemy import select
from sqlalchemy.orm import selectinload
from sqlalchemy.ext.asyncio import AsyncSession
from fastapi import HTTPException, status
from app.modules.families.models import User, Family, Household, FamilyMember
from app.core.security import AuthenticatedUser

def generate_invite_code(length: int = 8) -> str:
    chars = string.ascii_uppercase + string.digits
    return "".join(secrets.choice(chars) for _ in range(length))

class FamilyService:
    @staticmethod
    async def get_or_create_user(session: AsyncSession, auth_user: AuthenticatedUser) -> User:
        # 1. Try finding by UUID if valid UUID string
        user = None
        try:
            user_uuid = uuid.UUID(auth_user.uid)
            result = await session.execute(select(User).where(User.id == user_uuid))
            user = result.scalar_one_or_none()
        except ValueError:
            pass

        # 2. Try finding by firebase_uid
        if not user:
            result = await session.execute(select(User).where(User.firebase_uid == auth_user.uid))
            user = result.scalar_one_or_none()

        # 3. Try finding by email if present
        if not user and auth_user.email:
            result = await session.execute(select(User).where(User.email == auth_user.email.lower()))
            user = result.scalar_one_or_none()

        # 4. If still not found, create new user
        if not user:
            user = User(
                firebase_uid=auth_user.uid,
                email=auth_user.email.lower() if auth_user.email else None,
                phone=auth_user.phone
            )
            session.add(user)
            await session.commit()
            await session.refresh(user)
        return user

    @staticmethod
    async def create_family(
        session: AsyncSession,
        user: User,
        family_name: str,
        creator_name: str,
        household_name: str
    ) -> Family:
        invite_code = generate_invite_code()
        
        family = Family(
            name=family_name,
            invitation_code=invite_code,
            created_by=user.id
        )
        session.add(family)
        await session.flush()

        # Primary Household
        household = Household(
            family_id=family.id,
            name=household_name or "Main Home"
        )
        session.add(household)

        # Creator Family Member Profile
        admin_member = FamilyMember(
            family_id=family.id,
            user_id=user.id,
            display_name=creator_name,
            role="FAMILY_ADMIN",
            color_code="#2D3A34"
        )
        session.add(admin_member)

        await session.commit()

        # Re-fetch with loaded relations
        result = await session.execute(
            select(Family)
            .where(Family.id == family.id)
            .options(selectinload(Family.members), selectinload(Family.households))
        )
        return result.scalar_one()

    @staticmethod
    async def join_family(
        session: AsyncSession,
        user: User,
        invitation_code: str,
        display_name: str
    ) -> FamilyMember:
        result = await session.execute(
            select(Family).where(Family.invitation_code == invitation_code.strip().upper())
        )
        family = result.scalar_one_or_none()
        if not family:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Invalid family invitation code"
            )

        # Check if user is already a member
        member_check = await session.execute(
            select(FamilyMember).where(
                FamilyMember.family_id == family.id,
                FamilyMember.user_id == user.id
            )
        )
        if member_check.scalar_one_or_none():
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="You are already a member of this family"
            )

        member = FamilyMember(
            family_id=family.id,
            user_id=user.id,
            display_name=display_name,
            role="ADULT",
            color_code="#4E6058"
        )
        session.add(member)
        await session.commit()
        await session.refresh(member)
        return member

    @staticmethod
    async def get_user_families(session: AsyncSession, user_id: uuid.UUID) -> List[Family]:
        result = await session.execute(
            select(Family)
            .join(FamilyMember, FamilyMember.family_id == Family.id)
            .where(FamilyMember.user_id == user_id)
            .options(selectinload(Family.members), selectinload(Family.households))
        )
        return list(result.scalars().all())
