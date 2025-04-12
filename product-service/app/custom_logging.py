import logging
from logging.handlers import RotatingFileHandler
from fastapi import Request
import time
import os

LOG_FORMAT = "%(asctime)s - %(levelname)s - %(message)s"
LOG_FILE = "product-service.log"

def setup_logging():
    logging.basicConfig(
        level=logging.INFO,
        format=LOG_FORMAT,
        handlers=[
            RotatingFileHandler(
                LOG_FILE,
                maxBytes=1024*1024*5,  # 5MB
                backupCount=5
            ),
            logging.StreamHandler()
        ]
    )
    
    # SQLAlchemy query logging
    sql_logger = logging.getLogger('sqlalchemy.engine')
    sql_logger.setLevel(logging.WARNING)

logger = logging.getLogger(__name__)