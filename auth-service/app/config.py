from dotenv import load_dotenv
from pydantic import AnyHttpUrl, PostgresDsn, BaseSettings
from typing import List, Optional
import os

load_dotenv()

class Settings(BaseSettings):
    API_V1_AUTH: str = "/api/v1/auth"
    API_V1_HEALTH: str = "/api/v1/auth/ready"
    PROJECT_NAME: str = "Auth Service"
    
    # Security
    SECRET_KEY: str
    ALGORITHM: str = "HS256"
    API_KEY: str
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 100    
    SECRET_KEY = os.getenv("SECRET_KEY")
    ALGORITHM = os.getenv("ALGORITHM")
    ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES")) 

    CORS_ORIGINS: str = ""  # Comma-separated string of URLs

    #Service Discovery
    CONSUL_URL = "http://" + os.getenv('CONSUL_HOST') + ":" + os.getenv('CONSUL_PORT')

    @staticmethod
    def get_external_ip():
        return os.getenv("CONSUL_HOST")
    
    @property
    def cors_origins_list(self) -> List[str]:
        """Parse CORS_ORIGINS string into a list of URLs"""
        if not self.CORS_ORIGINS:
            return []
        return [origin.strip() for origin in self.CORS_ORIGINS.split(",") if origin.strip()]
    
    class Config:
        env_file=".env"
        case_sensitive=True   

settings = Settings()