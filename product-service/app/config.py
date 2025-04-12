#from pydantic_settings import BaseSettings
from pydantic import AnyHttpUrl, PostgresDsn, BaseSettings
from typing import List, Optional
from app.custom_logging import logger
import os


class Settings(BaseSettings):
    API_V1_PRODUCT: str = "/api/v1/products"
    API_V1_HEALTH: str = "/api/v1/products/health"
    PROJECT_NAME: str = "Product Service"

    # Database
    DB_USER: str 
    DB_PASSWORD: str 
    DB_HOST: str 
    DB_PORT: str = "5432"
    DB_NAME: str
    DB_POOL_SIZE: int = 10
    DB_MAX_OVERFLOW: int = 20
    DB_POOL_TIMEOUT: int = 30
    DB_POOL_RECYCLE: int = 3600
    
    # Security
    SECRET_KEY: str
    ALGORITHM: str = "HS256"
    API_KEY: str
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30    

    CORS_ORIGINS: str = ""  # Comma-separated string of URLs

    #Service Discovery
    CONSUL_URL = "http://" + os.getenv('CONSUL_HOST') + ":" + os.getenv('CONSUL_PORT')
    
    @property
    def cors_origins_list(self) -> List[str]:
        """Parse CORS_ORIGINS string into a list of URLs"""
        if not self.CORS_ORIGINS:
            return []
        return [origin.strip() for origin in self.CORS_ORIGINS.split(",") if origin.strip()]

    # Observability
    PROMETHEUS_ENABLED: bool = True
    
    class Config:
        env_file=".env"
        case_sensitive=True

    @staticmethod
    def get_external_ip():
        return os.getenv("EXTERNAL_HOST_IP")

settings = Settings()

