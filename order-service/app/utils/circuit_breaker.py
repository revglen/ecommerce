import time
import asyncio
from typing import Optional
from contextlib import asynccontextmanager
from fastapi import HTTPException
import logging

logger = logging.getLogger(__name__)

class CircuitBreakerException(HTTPException):
    def __init__(self):
        super().__init__(
            status_code=503,
            detail="Service unavailable due to high failure rate"
        )

class CircuitBreaker:
    def __init__(self, failure_threshold=5, recovery_timeout=30):
        self.failure_threshold = failure_threshold
        self.recovery_timeout = recovery_timeout
        self.failure_count = 0
        self.last_failure_time: Optional[float] = None
        self._lock = asyncio.Lock()
        self.state = "closed"
        logger.info(f"Circuit breaker initialized with threshold {failure_threshold} and timeout {recovery_timeout}")

    async def is_open(self):
        async with self._lock:
            if self.last_failure_time is None:
                self.state = "closed"
                return False
            
            current_time = time.time()
            if current_time - self.last_failure_time > self.recovery_timeout:
                self.failure_count = 0
                self.last_failure_time = None
                self.state = "half-open"
                return False
            
            if self.failure_count >= self.failure_threshold:
                self.state = "open"
                return True
            
            self.state = "closed"
            return False

    async def record_failure(self):
        async with self._lock:
            self.failure_count += 1
            self.last_failure_time = time.time()
            logger.warning(f"Circuit breaker recorded failure (count: {self.failure_count})")

    async def record_success(self):
        async with self._lock:
            self.failure_count = 0
            self.last_failure_time = None
            logger.info("Circuit breaker reset after successful operation")

    @asynccontextmanager
    async def protect(self):
        if await self.is_open():
            logger.error("Circuit breaker is open, rejecting request")
            raise CircuitBreakerException()
        
        try:
            yield
            await self.record_success()
        except Exception as e:
            await self.record_failure()
            logger.error(f"Operation failed: {str(e)}")
            raise