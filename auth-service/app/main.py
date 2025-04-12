import requests
import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

from app.routers import auth, health
from app.custom_logging import setup_logging, logger
from app.middleware.logging_middleware import log_requests, LoggingRoute
from app.config import Settings,settings
from app.utils.observability import setup_observability
from app.authentication import Auth
from prometheus_fastapi_instrumentator import Instrumentator # type: ignore

# Initialize logging first
setup_logging()

print("Service Discovery:" + settings.CONSUL_URL)

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Register service
    print("Service Registration initiated...")
    service_id = f"{app.title}-{os.getenv('HOSTNAME')}"
    registration = {
        "ID":"auth-service",
        "Name": "auth-service",
        "Address": Settings.get_external_ip(),
        "Port": 8002,
        "Check": {
            "HTTP": f"http://{Settings.get_external_ip()}:8002/api/v1/auth/ready",
            "Interval": "30s",
            "Timeout": "5s"
        }
    }

    print("Registration:")
    print(registration)

    #Register with Service Discovery
    response = requests.put(f"{settings.CONSUL_URL}/v1/agent/service/register", json=registration)
    if response.ok:
        print("Update successful!")
        try:
            if response.json() is not None:
                print("Response data:", response.json())
        except:
            pass
    else:
        print(f"Error {response.status_code}: {response.text}")
    
    yield
    
    # Deregister
    requests.put(f"{settings.CONSUL_URL}/v1/agent/service/deregister/{service_id}")

app = FastAPI(
    lifespan=lifespan,
    title="auth Service",
    description="Microservice for auth management",
    version="1.0.0",
    openapi_url=f"{settings.API_V1_AUTH}/openapi.json",
    docs_url=f"{settings.API_V1_AUTH}/docs",
    redoc_url=f"{settings.API_V1_AUTH}/redoc"  
)

# Add middleware
app.middleware("http")(log_requests)

# Configure routers to use logging route class
app.router.route_class = LoggingRoute

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router, prefix=settings.API_V1_AUTH)
app.include_router(health.router, prefix=settings.API_V1_HEALTH)


setup_observability(app)

instrumentator = Instrumentator(
    excluded_handlers=["/metrics", "/health"],
    should_group_status_codes=True,
    should_ignore_untemplated=True,
    should_respect_env_var=True,
    env_var_name="ENABLE_METRICS",
)

@app.on_event("startup")
async def startup_event():
    logger.info("Starting auth Service")
    instrumentator.instrument(app).expose(app)

@app.on_event("shutdown")
async def shutdown_event():
    logger.info("Shutting down auth Service")
    
if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=8002, reload=True)
