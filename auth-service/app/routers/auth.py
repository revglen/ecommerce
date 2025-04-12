from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from datetime import datetime, timedelta
from jose import JWTError, jwt # type: ignore
from dotenv import load_dotenv

from app.dependencies import get_api_key
from app.custom_logging import logger
from app.authentication import Auth, Token
from app.config import settings
from app.custom_logging import logger

load_dotenv()
router = APIRouter(dependencies=[Depends(get_api_key)])

@router.post("/token", response_model=Token)
async def login_for_access_token(form_data: OAuth2PasswordRequestForm = Depends()):
    user = Auth.authenticate_user(form_data.username, form_data.password)
    if not user:
        logger.info("User not authenticated")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = Auth.create_access_token(
        data={"sub": user["username"]}, expires_delta=access_token_expires
    )

    logger.info("User authenticated and token being returned")
    return {"access_token": access_token, "token_type": "bearer"}

@router.get("/validate-token")
async def validate_token(token: str = Depends(Auth.oAuth2_scheme)):
    try:
        print("The token is " + token)
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
        username: str = payload.get("sub")
        if username is None:
            logger.error("Invalid Token")
            raise HTTPException(status_code=401, detail="Invalid token")
        
        logger.info("Token is valid")
        return {"username": username}
    except JWTError:
        logger.error("Error generated while decoding")
        raise HTTPException(status_code=401, detail="Invalid token")