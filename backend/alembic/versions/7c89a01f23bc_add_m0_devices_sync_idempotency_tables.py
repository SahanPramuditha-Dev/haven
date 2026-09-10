"""add_m0_devices_sync_idempotency_tables

Revision ID: 7c89a01f23bc
Revises: f5b3455a97c3
Create Date: 2026-09-09 20:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects.postgresql import UUID, JSONB

revision: str = '7c89a01f23bc'
down_revision: Union[str, Sequence[str], None] = 'f5b3455a97c3'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Devices table (Multi-Device FCM Token Registration)
    op.create_table(
        'devices',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('user_id', sa.String(36), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
        sa.Column('installation_id', sa.String(64), unique=True, index=True, nullable=False),
        sa.Column('fcm_token', sa.String(512), nullable=False),
        sa.Column('platform', sa.String(20), server_default='android', nullable=False),
        sa.Column('device_name', sa.String(100), nullable=True),
        sa.Column('notifications_enabled', sa.Boolean(), server_default='true', nullable=False),
        sa.Column('last_seen_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column('revoked_at', sa.DateTime(timezone=True), nullable=True)
    )
    op.create_index('idx_devices_user_id', 'devices', ['user_id'])

    # 2. Unified Append-Only Sync Events Log
    op.create_table(
        'sync_events',
        sa.Column('sequence', sa.BigInteger().with_variant(sa.Integer, 'sqlite'), primary_key=True, autoincrement=True),
        sa.Column('family_id', sa.String(36), sa.ForeignKey('families.id', ondelete='CASCADE'), nullable=False),
        sa.Column('entity_type', sa.String(50), nullable=False),
        sa.Column('entity_id', sa.String(36), nullable=False),
        sa.Column('operation', sa.String(20), nullable=False),
        sa.Column('revision', sa.Integer(), server_default='1', nullable=False),
        sa.Column('actor_user_id', sa.String(36), sa.ForeignKey('users.id', ondelete='SET NULL'), nullable=True),
        sa.Column('payload', sa.JSON(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False)
    )
    op.create_index('idx_sync_events_family_seq', 'sync_events', ['family_id', 'sequence'])

    # 3. Scoped Idempotency Records Table
    op.create_table(
        'idempotency_records',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('user_id', sa.String(36), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
        sa.Column('operation_id', sa.String(36), nullable=False),
        sa.Column('route', sa.String(255), nullable=False),
        sa.Column('request_hash', sa.String(64), nullable=False),
        sa.Column('status', sa.String(20), server_default='COMPLETED', nullable=False),
        sa.Column('resource_id', sa.String(36), nullable=True),
        sa.Column('response_code', sa.Integer(), nullable=False),
        sa.Column('response_body', sa.JSON(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column('expires_at', sa.DateTime(timezone=True), nullable=False),
        sa.UniqueConstraint('user_id', 'operation_id', name='uq_user_operation')
    )
    op.create_index('idx_idempotency_user_op', 'idempotency_records', ['user_id', 'operation_id'])
    op.create_index('idx_idempotency_expires', 'idempotency_records', ['expires_at'])


def downgrade() -> None:
    op.drop_table('idempotency_records')
    op.drop_table('sync_events')
    op.drop_table('devices')
