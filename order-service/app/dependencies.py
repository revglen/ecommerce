from fastapi import Depends, HTTPException, status
from fastapi.security import APIKeyHeader
from typing import Optional
from app.config import settings
from jose import JWTError, jwt
from app.database import get_db
from sqlalchemy.orm import Session
import logging
from app.utils import security

logger = logging.getLogger(__name__)


api_key_header = APIKeyHeader(name="X-API-Key")

def get_api_key(api_key: str = Depends(api_key_header)):
   
    if api_key != settings.SECRET_KEY:
        logger.warning("Invalid token attempt")
        raise HTTPException(            
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Invalid API Key"
        )
    return api_key


get_token_header = APIKeyHeader(name="Authorization")

def verify_token(token:str=Depends(get_token_header), api_key: str = Depends(get_api_key)) -> bool:
    try:
       print("The token: " + token)
       print ("API Key: " + api_key)
       
       user:str = security.SecurityUtil().get_current_user(token, api_key)
       
       print("The user: " + user)
       return user
    except JWTError:
       logger.warning("Invalid token attempt")
       raise HTTPException( status_code=status.HTTP_401_UNAUTHORIZED,
           detail="Invalid token"
       )
    except HTTPException:
       logger.warning("Token validation failed")
       raise HTTPException( status_code=status.HTTP_401_UNAUTHORIZED,
           detail="Invalid token"
       )
