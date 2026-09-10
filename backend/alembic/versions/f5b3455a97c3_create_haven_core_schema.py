"""create_haven_core_schema

Revision ID: f5b3455a97c3
Revises: 
Create Date: 2026-09-06 03:05:11.173546

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects.postgresql import UUID

revision: str = 'f5b3455a97c3'
down_revision: Union[str, Sequence[str], None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Users table
    op.create_table(
        'users',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('firebase_uid', sa.String(128), unique=True, index=True, nullable=False),
        sa.Column('email', sa.String(255), unique=True, nullable=True),
        sa.Column('phone', sa.String(32), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now()),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.func.now())
    )

    # 2. Families table
    op.create_table(
        'families',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('name', sa.String(100), nullable=False),
        sa.Column('invitation_code', sa.String(16), unique=True, index=True, nullable=False),
        sa.Column('created_by', sa.String(36), sa.ForeignKey('users.id'), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now()),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.func.now())
    )

    # 3. Households table
    op.create_table(
        'households',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('family_id', sa.String(36), sa.ForeignKey('families.id', ondelete='CASCADE'), nullable=False),
        sa.Column('name', sa.String(100), nullable=False),
        sa.Column('address', sa.String(255), nullable=True),
        sa.Column('latitude', sa.Float(), nullable=True),
        sa.Column('longitude', sa.Float(), nullable=True),
        sa.Column('radius_meters', sa.Integer(), default=100),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now())
    )

    # 4. Family Members table (Decoupled User != FamilyMember)
    op.create_table(
        'family_members',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('family_id', sa.String(36), sa.ForeignKey('families.id', ondelete='CASCADE'), nullable=False),
        sa.Column('user_id', sa.String(36), sa.ForeignKey('users.id', ondelete='SET NULL'), nullable=True),
        sa.Column('display_name', sa.String(100), nullable=False),
        sa.Column('role', sa.String(32), default='PARENT_GUARDIAN'),
        sa.Column('avatar_url', sa.String(512), nullable=True),
        sa.Column('date_of_birth', sa.Date(), nullable=True),
        sa.Column('color_code', sa.String(9), default='#4E6058'),
        sa.Column('is_active', sa.Boolean(), default=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now())
    )

    # 5. Channels table
    op.create_table(
        'channels',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('family_id', sa.String(36), sa.ForeignKey('families.id', ondelete='CASCADE'), nullable=False),
        sa.Column('name', sa.String(100), nullable=False),
        sa.Column('channel_type', sa.String(32), default='general'),
        sa.Column('is_encrypted', sa.Boolean(), default=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now())
    )

    # 6. Messages table
    op.create_table(
        'messages',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('channel_id', sa.String(36), sa.ForeignKey('channels.id', ondelete='CASCADE'), nullable=False),
        sa.Column('sender_id', sa.String(36), nullable=False),
        sa.Column('sender_name', sa.String(100), nullable=False),
        sa.Column('content', sa.Text(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now())
    )

    # 7. Tasks table
    op.create_table(
        'tasks',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('family_id', sa.String(36), sa.ForeignKey('families.id', ondelete='CASCADE'), nullable=False),
        sa.Column('title', sa.String(255), nullable=False),
        sa.Column('assigned_to', sa.String(100), nullable=False),
        sa.Column('is_completed', sa.Boolean(), default=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now())
    )

    # 8. Calendar Events table
    op.create_table(
        'calendar_events',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('family_id', sa.String(36), sa.ForeignKey('families.id', ondelete='CASCADE'), nullable=False),
        sa.Column('title', sa.String(255), nullable=False),
        sa.Column('start_time', sa.String(32), nullable=False),
        sa.Column('end_time', sa.String(32), nullable=True),
        sa.Column('date', sa.String(32), nullable=False),
        sa.Column('location', sa.String(255), default=''),
        sa.Column('attendee', sa.String(100), default='All Family'),
        sa.Column('category', sa.String(64), default='Routine'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now())
    )

    # 9. Safety Zones table
    op.create_table(
        'safety_zones',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('family_id', sa.String(36), sa.ForeignKey('families.id', ondelete='CASCADE'), nullable=False),
        sa.Column('name', sa.String(100), nullable=False),
        sa.Column('subtitle', sa.String(255), default=''),
        sa.Column('zone_type', sa.String(32), default='home'),
        sa.Column('is_active', sa.Boolean(), default=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now())
    )

    # 10. SOS Alerts table
    op.create_table(
        'sos_alerts',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('family_id', sa.String(36), sa.ForeignKey('families.id', ondelete='CASCADE'), nullable=False),
        sa.Column('sender_name', sa.String(100), nullable=False),
        sa.Column('latitude', sa.Float(), nullable=False),
        sa.Column('longitude', sa.Float(), nullable=False),
        sa.Column('status', sa.String(32), default='ACTIVE'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now())
    )

    # 11. Family Expenses table
    op.create_table(
        'family_expenses',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('family_id', sa.String(36), sa.ForeignKey('families.id', ondelete='CASCADE'), nullable=False),
        sa.Column('title', sa.String(255), nullable=False),
        sa.Column('amount', sa.Float(), nullable=False),
        sa.Column('category', sa.String(64), nullable=False),
        sa.Column('paid_by', sa.String(100), nullable=False),
        sa.Column('split_type', sa.String(32), default='Equal'),
        sa.Column('date', sa.String(32), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now())
    )

    # 12. Vault Documents table
    op.create_table(
        'vault_documents',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('family_id', sa.String(36), sa.ForeignKey('families.id', ondelete='CASCADE'), nullable=False),
        sa.Column('title', sa.String(255), nullable=False),
        sa.Column('doc_type', sa.String(64), nullable=False),
        sa.Column('encrypted_blob', sa.Text(), nullable=False),
        sa.Column('uploaded_by', sa.String(100), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now())
    )


def downgrade() -> None:
    op.drop_table('vault_documents')
    op.drop_table('family_expenses')
    op.drop_table('sos_alerts')
    op.drop_table('safety_zones')
    op.drop_table('calendar_events')
    op.drop_table('tasks')
    op.drop_table('messages')
    op.drop_table('channels')
    op.drop_table('family_members')
    op.drop_table('households')
    op.drop_table('families')
    op.drop_table('users')
