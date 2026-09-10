from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.core.database import get_db
from app.modules.finance.models import FamilyExpense, VaultDocument
from app.modules.finance.schemas import (
    FamilyExpenseCreate,
    FamilyExpenseResponse,
    VaultDocumentCreate,
    VaultDocumentResponse
)

router = APIRouter(prefix="/families/{family_id}/finance", tags=["finance"])

@router.get("/expenses", response_model=List[FamilyExpenseResponse])
async def get_expenses(
    family_id: str,
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(
        select(FamilyExpense)
        .where(FamilyExpense.family_id == family_id)
        .order_by(FamilyExpense.created_at.desc())
    )
    expenses = result.scalars().all()
    if not expenses:
        defaults = [
            FamilyExpense(
                family_id=family_id,
                title="Whole Foods Groceries",
                amount=142.50,
                category="Groceries",
                paid_by="Alice",
                split_type="Equal",
                date="Today"
            ),
            FamilyExpense(
                family_id=family_id,
                title="City High-Speed Internet",
                amount=79.99,
                category="Utilities",
                paid_by="Bob",
                split_type="Equal",
                date="Yesterday"
            ),
            FamilyExpense(
                family_id=family_id,
                title="Charlie Soccer Gear",
                amount=85.00,
                category="Kids",
                paid_by="Alice",
                split_type="Family",
                date="Sep 4"
            )
        ]
        for d in defaults:
            db.add(d)
        await db.commit()
        for d in defaults:
            await db.refresh(d)
        return defaults
    return expenses

@router.post("/expenses", response_model=FamilyExpenseResponse, status_code=status.HTTP_201_CREATED)
async def create_expense(
    family_id: str,
    payload: FamilyExpenseCreate,
    db: AsyncSession = Depends(get_db)
):
    exp = FamilyExpense(
        family_id=family_id,
        title=payload.title,
        amount=payload.amount,
        category=payload.category,
        paid_by=payload.paid_by,
        split_type=payload.split_type,
        date=payload.date
    )
    db.add(exp)
    await db.commit()
    await db.refresh(exp)
    return exp

@router.get("/vault", response_model=List[VaultDocumentResponse])
async def get_vault_documents(
    family_id: str,
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(
        select(VaultDocument)
        .where(VaultDocument.family_id == family_id)
        .order_by(VaultDocument.created_at.desc())
    )
    docs = result.scalars().all()
    if not docs:
        defaults = [
            VaultDocument(
                family_id=family_id,
                title="Family Homeowners Policy",
                doc_type="Insurance",
                encrypted_blob="ENC:AES256:4f8a92...[E2E_VERIFIED]",
                uploaded_by="Alice"
            ),
            VaultDocument(
                family_id=family_id,
                title="Charlie Immunization Records",
                doc_type="Medical",
                encrypted_blob="ENC:AES256:7c1d33...[E2E_VERIFIED]",
                uploaded_by="Bob"
            )
        ]
        for d in defaults:
            db.add(d)
        await db.commit()
        for d in defaults:
            await db.refresh(d)
        return defaults
    return docs

@router.post("/vault", response_model=VaultDocumentResponse, status_code=status.HTTP_201_CREATED)
async def store_vault_document(
    family_id: str,
    payload: VaultDocumentCreate,
    db: AsyncSession = Depends(get_db)
):
    doc = VaultDocument(
        family_id=family_id,
        title=payload.title,
        doc_type=payload.doc_type,
        encrypted_blob=payload.encrypted_blob,
        uploaded_by=payload.uploaded_by
    )
    db.add(doc)
    await db.commit()
    await db.refresh(doc)
    return doc
