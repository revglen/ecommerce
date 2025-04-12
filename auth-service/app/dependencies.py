from fastapi import Depends, HTTPException, status
from fastapi.security import APIKeyHeader
from typing import Optional
from app.config import settings
from jose import JWTError, jwt # type: ignore
from sqlalchemy.orm import Session
from app.custom_logging import logger

api_key_header = APIKeyHeader(name="X-API-Key")

def get_api_key(api_key: str = Depends(api_key_header)):
   
    if api_key != settings.SECRET_KEY:
        logger.warning("Invalid token attempt")
        raise HTTPException(            
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Invalid API Key"
        )
    return api_key


#get_api_key = APIKeyHeader(name="X-API-Key")

#def verify_token(token:str=Depends(get_api_key)):
#    try:
#        payload=jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM]) 
#        return payload
#    except JWTError:
#        logger.warning("Invalid token attempt")
#        raise HTTPException( status_code=status.HTTP_401_UNAUTHORIZED,
#            detail="Invalid token"
#        )
    
#def get_current_user(
#        token:str = Depends(verify_token),
#        db:Session=Depends(get_db)):
#    pass

