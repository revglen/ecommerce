#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER order_user WITH PASSWORD 'order_password';
    CREATE DATABASE "order-db" WITH OWNER order_user;
    GRANT ALL PRIVILEGES ON DATABASE "order-db" TO order_user;
EOSQL