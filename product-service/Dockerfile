FROM python:3.9-slim

# Set environment variables
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONPATH=/app \
    DB_HOST=postgres \
    DB_PORT=5432 \
    DB_NAME=product_db \
    DB_USER=product_user \
    DB_PASSWORD=admin

WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends gcc python3-dev && \
    rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
#RUN pip install --no-cache-dir -r requirements1.txt

RUN pip install --upgrade pip && \
    pip install --upgrade-strategy only-if-needed -r requirements.txt

COPY . .

#RUN python -m pytest tests/
RUN pip install -e .

EXPOSE 8000

#CMD ["sh", "-c", "alembic upgrade head && uvicorn app.main:app --host 0.0.0.0 --port 8000"]
CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port 8000"]

