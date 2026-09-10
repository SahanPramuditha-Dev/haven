import uuid
from datetime import datetime
from sqlalchemy import Column, String, DateTime, ForeignKey, Float, Boolean
from app.core.database import Base

class FamilyExpense(Base):
    __tablename__ = "family_expenses"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    family_id = Column(String, ForeignKey("families.id"), nullable=False)
    title = Column(String, nullable=False)
    amount = Column(Float, nullable=False)
    category = Column(String, nullable=False) # Groceries, Utilities, Housing, Kids, Leisure
    paid_by = Column(String, nullable=False)
    split_type = Column(String, nullable=False, default="Equal")
    date = Column(String, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)

class VaultDocument(Base):
    __tablename__ = "vault_documents"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    family_id = Column(String, ForeignKey("families.id"), nullable=False)
    title = Column(String, nullable=False)
    doc_type = Column(String, nullable=False) # Insurance, ID, Medical, Property
    encrypted_blob = Column(String, nullable=False) # AES-256 encrypted payload
    uploaded_by = Column(String, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)
