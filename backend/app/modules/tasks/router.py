from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.modules.tasks.models import Task
from app.modules.tasks.schemas import TaskCreate, TaskResponse

router = APIRouter(prefix="/tasks", tags=["Tasks"])

@router.get("/{family_id}", response_model=List[TaskResponse])
async def get_tasks(family_id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(Task).where(Task.family_id == family_id).order_by(Task.created_at.asc())
    )
    tasks = list(result.scalars().all())
    if not tasks:
        defaults = [
            Task(family_id=family_id, title="Feed Luna (Cat)", assigned_to="Charlie", is_completed=False),
            Task(family_id=family_id, title="Pick up groceries from market", assigned_to="Bob", is_completed=False),
            Task(family_id=family_id, title="Confirm dentist appointment", assigned_to="Alice", is_completed=True)
        ]
        for t in defaults:
            db.add(t)
        await db.commit()
        result = await db.execute(
            select(Task).where(Task.family_id == family_id).order_by(Task.created_at.asc())
        )
        tasks = list(result.scalars().all())
    return tasks

@router.post("/{family_id}", response_model=TaskResponse, status_code=status.HTTP_201_CREATED)
async def create_task(family_id: str, task_in: TaskCreate, db: AsyncSession = Depends(get_db)):
    task = Task(
        family_id=family_id,
        title=task_in.title,
        assigned_to=task_in.assigned_to,
        is_completed=False
    )
    db.add(task)
    await db.commit()
    await db.refresh(task)
    return task

@router.patch("/{family_id}/{task_id}/toggle", response_model=TaskResponse)
async def toggle_task(family_id: str, task_id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(Task).where(Task.family_id == family_id, Task.id == task_id)
    )
    task = result.scalar_one_or_none()
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    task.is_completed = not task.is_completed
    await db.commit()
    await db.refresh(task)
    return task
