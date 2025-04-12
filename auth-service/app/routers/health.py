from fastapi import APIRouter

from fastapi.responses import JSONResponse
from fastapi import Depends

from app.config import settings
from app.custom_logging import logger

router = APIRouter()

@router.get("/")
async def liveness_check():
    return {"status":"alive"} 