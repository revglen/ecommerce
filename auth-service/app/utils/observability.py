from fastapi import FastAPI
from prometheus_client import make_asgi_app, Counter, Histogram, Gauge # type: ignore
from app.custom_logging import logger
import time

# Metrics
REQUEST_COUNT = Counter(
    'http_requests_total',
    'Total HTTP Requests',
    ['method', 'endpoint', 'status']
)

REQUEST_LATENCY = Histogram(
    'http_request_duration_seconds',
    'HTTP request latency',
    ['method', 'endpoint']
)

DB_QUERY_TIME = Histogram(
    'db_query_duration_seconds',
    'Database query duration',
    ['query_type']
)

def setup_observability(app: FastAPI):
    # Add Prometheus metrics endpoint
    metrics_app = make_asgi_app()
    app.mount("/metrics", metrics_app)
    
    # Middleware for request metrics
    @app.middleware("http")
    async def observe_requests(request, call_next):
        start_time = time.time()
        method = request.method
        endpoint = request.url.path
        
        try:
            response = await call_next(request)
            REQUEST_COUNT.labels(method, endpoint, response.status_code).inc()
            REQUEST_LATENCY.labels(method, endpoint).observe(time.time() - start_time)
            return response
        except Exception as e:
            REQUEST_COUNT.labels(method, endpoint, 500).inc()
            raise
    
    logger.info("Observability setup complete")