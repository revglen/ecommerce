FROM python:3.9-slim

# Set environment variables
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONPATH=/app

WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends  libpq-dev gcc python3-dev curl  && \
    rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
COPY .env ./
RUN set -a && . ./.env && set +a
#RUN pip install --no-cache-dir -r requirements1.txt

RUN pip install --upgrade pip && \
    pip install --upgrade-strategy only-if-needed -r requirements.txt

COPY . .

#RUN python -m pytest tests/
RUN pip install -e .

EXPOSE 8002

#CMD ["sh", "-c", "alembic upgrade head && uvicorn app.main:app --host 0.0.0.0 --port 8000"]
CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port 8002"]

