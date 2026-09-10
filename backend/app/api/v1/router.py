from fastapi import APIRouter
from app.modules.families.router import router as families_router
from app.modules.chat.router import router as chat_router
from app.modules.tasks.router import router as tasks_router
from app.modules.calendar.router import router as calendar_router
from app.modules.safety.router import router as safety_router
from app.modules.finance.router import router as finance_router
from app.modules.auth.router import router as auth_router

api_router = APIRouter(prefix="/api/v1")
api_router.include_router(auth_router)
api_router.include_router(auth_router, prefix="/users", tags=["Users"])
api_router.include_router(families_router)
api_router.include_router(chat_router)
api_router.include_router(tasks_router)
api_router.include_router(calendar_router)
api_router.include_router(safety_router)
api_router.include_router(finance_router)
