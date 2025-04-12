from fastapi import APIRouter

#from fastapi.response import JSONResponse
from fastapi import FastAPI, Response
from fastapi.responses import JSONResponse
from fastapi import Depends

from sqlalchemy import text
from app.database import get_db
from sqlalchemy.orm import Session
from app.config import settings
from app.custom_logging import logger

router = APIRouter(include_in_schema=False)

@router.get("/live")
async def liveness_check():
    return {"status":"alive"}

@router.get("/ready")
async def readiness_check(db:Session=Depends(get_db)):
    try:
        db.execute(text("SELECT 1"))
        return {
            "status":"ready",
            "services": {
                "databasw":"ok",
                "version": settings.PROJECT_NAME
            }
        }
    except Exception as e:
        logger.error(f"Readiness check failed: {str(e)}")
        return JSONResponse(
            status_code=503,
            content={"status": "not ready", "error": str(e)}
        )