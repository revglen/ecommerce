#!/bin/bash

rm -rf .env
cp .env.template .env

echo -e "\n" >> .env
echo "EXTERNAL_HOST_IP=$(hostname -I | awk '{print $1}')" >> .env

# Run docker-compose with the environment variable
sudo docker compose down -v 
sudo docker compose up --build