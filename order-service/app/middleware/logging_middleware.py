from fastapi import Request
from fastapi.routing import APIRoute
from typing import Callable
from ..custom_logging import logger
import time

async def log_requests(request: Request, call_next):
    start_time = time.time()
    response = await call_next(request)
    process_time = (time.time() - start_time) * 1000
    
    log_data = {
        "method": request.method,
        "path": request.url.path,
        "status": response.status_code,
        "ip": request.client.host if request.client else None,
        "process_time": f"{process_time:.2f}ms"
    }
    
    if response.status_code >= 400:
        logger.error("Request failed", extra=log_data)
    else:
        logger.info("Request successful", extra=log_data)
    
    return response

class LoggingRoute(APIRoute):
    def get_route_handler(self) -> Callable:
        original_route_handler = super().get_route_handler()

        async def custom_route_handler(request: Request):
            try:
                return await original_route_handler(request)
            except Exception as exc:
                logger.error(
                    "Unhandled exception occurred",
                    exc_info=(type(exc), exc, exc.__traceback__),
                    extra={
                        "method": request.method,
                        "path": request.url.path
                    }
                )
                raise

        return custom_route_handler