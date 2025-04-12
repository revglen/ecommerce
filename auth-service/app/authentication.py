from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from pydantic import BaseModel
from datetime import datetime, timedelta
from jose import JWTError, jwt # type: ignore
from passlib.context import CryptContext # type: ignore
from dotenv import load_dotenv

from app.dependencies import get_api_key
from app.custom_logging import logger
from app.config import settings

load_dotenv()
router = APIRouter(dependencies=[Depends(get_api_key)])

class Token(BaseModel):
    access_token: str
    token_type: str

class TokenData(BaseModel):
    username: str = None

class Auth:

    pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
    oAuth2_scheme = OAuth2PasswordBearer(tokenUrl="token")

    @staticmethod
    def verify_password(plain_password, hashed_password):
        return Auth.pwd_context.verify(plain_password, hashed_password)

    @staticmethod
    def get_password_hash(password):
        return Auth.pwd_context.hash(password)

    @staticmethod
    def authenticate_user(username: str, password: str):
        # Replace with DB query in production
        fake_user = {"username": "admin", "hashed_password": Auth.get_password_hash("adminpass")}
        if username != fake_user["username"]:
            return False
        if not Auth.verify_password(password, fake_user["hashed_password"]):
            return False
        return fake_user

    @staticmethod
    def create_access_token(data: dict, expires_delta: timedelta = None):
        to_encode = data.copy()
        if expires_delta:
            expire = datetime.utcnow() + expires_delta
        else:
            expire = datetime.utcnow() + timedelta(minutes=15)
        to_encode.update({"exp": expire})
        encoded_jwt = jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)
        return encoded_jwt