import os
from fastapi import HTTPException
from fastapi import FastAPI, Depends, HTTPException, Header
from pydantic import BaseModel
import requests # type: ignore
import consul
from app.custom_logging import logger

class TokenData(BaseModel):
    username: str
    
class SecurityUtil:
     
    def __init__(self, username=""):
        self.username = username
        self.consul_host = os.getenv("CONSUL_HOST", "consul-server")
        self.consul_port = int(os.getenv("CONSUL_PORT", 8500))
        self.consul_client = consul.Consul(host=self.consul_host, port=self.consul_port)
    
    def get_auth_service_url(self) -> str:

        """Discover auth service URL from Consul"""
        _, services = self.consul_client.catalog.service("auth-service")
        if not services:
            logger.error("Auth service not available")
            raise HTTPException(
                status_code=503,
                detail="Auth service not available"
            )
        
        # Get the first healthy instance (you can add load balancing logic)
        auth_instance = services[0]
        consul_url = f"http://{auth_instance['ServiceAddress']}:{auth_instance['ServicePort']}"
        logger.info(consul_url)
        return consul_url
    
    def get_current_user(self, authorization: str, api_key: str) -> str:
        """Validate token with auth service via Consul"""

        if not authorization.startswith("Bearer "):
            raise HTTPException(
                status_code=401,
                detail="Invalid authorization header"
            )
        
        token = authorization.split(" ")[1]
        auth_url = self.get_auth_service_url()
        
        try:
            # Call auth service's validate endpoint
            response = requests.get(
                f"{auth_url}/api/v1/auth/validate-token",
                headers={"Authorization": f"Bearer {token}",
                         "X-API-Key": api_key,
                         "Accept": "application/json"}
            )
            response.raise_for_status()
            return TokenData(**response.json()).username
        except requests.exceptions.RequestException as e:
            raise HTTPException(
                status_code=502,
                detail=f"Auth service error: {str(e)}"
            )