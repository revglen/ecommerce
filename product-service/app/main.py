import requests
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
from app.routers import products, health
from app.database import engine
from app.models import Base
from app.config import Settings, settings
from app.utils.observability import setup_observability
from prometheus_fastapi_instrumentator import Instrumentator
import os
from app.custom_logging import logger
from contextlib import asynccontextmanager

print("Service Discovery:" + settings.CONSUL_URL)

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Register service
    print("Service Registration initiated...")
    service_id = f"{app.title}-{os.getenv('HOSTNAME')}"
    registration = {
        #"ID": service_id,
        "ID":"product-service",
        "Name": "product-service",
        "Address": Settings.get_external_ip(),
        "Port": 8000,
        "Check": {
            "HTTP": f"http://{Settings.get_external_ip()}:8000/api/v1/products/health/ready",
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
    title="Product Service",
    description="Microservice for product management",
    version="1.0.0",
    openapi_url=f"{settings.API_V1_PRODUCT}/openapi.json",
    docs_url=f"{settings.API_V1_PRODUCT}/docs",
    redoc_url=f"{settings.API_V1_PRODUCT}/redoc"  
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

Base.metadata.create_all(bind=engine)

app.include_router(products.router, prefix=settings.API_V1_PRODUCT)
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
    logger.info("Starting Product Service")
    instrumentator.instrument(app).expose(app, should_gzip=False, should_skip=False)

@app.on_event("shutdown")
async def shutdown_event():
    logger.info("Shutting down Product Service")
    
if __name__ == "__main__":
   uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
