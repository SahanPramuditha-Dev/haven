from pydantic import BaseModel
from typing import Optional
from datetime import datetime

class FamilyExpenseCreate(BaseModel):
    title: str
    amount: float
    category: str
    paid_by: str
    split_type: str = "Equal"
    date: str

class FamilyExpenseResponse(BaseModel):
    id: str
    family_id: str
    title: str
    amount: float
    category: str
    paid_by: str
    split_type: str
    date: str
    created_at: datetime

    class Config:
        from_attributes = True

class VaultDocumentCreate(BaseModel):
    title: str
    doc_type: str
    encrypted_blob: str
    uploaded_by: str

class VaultDocumentResponse(BaseModel):
    id: str
    family_id: str
    title: str
    doc_type: str
    encrypted_blob: str
    uploaded_by: str
    created_at: datetime

    class Config:
        from_attributes = True
