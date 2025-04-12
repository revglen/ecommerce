#!/bin/bash

rm -rf .env
cp .env.template .env

echo -e "\n" >> .env
echo "CONSUL_HOST=$(hostname -I | awk '{print $1}')" >> .env

# Run docker-compose with the environment variable
sudo docker compose down -v 
sudo docker compose up --build

#uvicorn app.main:app --reload --host 0.0.0.0 --port 8002
