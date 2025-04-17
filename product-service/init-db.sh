#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER product_user WITH PASSWORD 'product_password';
    CREATE DATABASE "product_db" WITH OWNER product_user;
    GRANT ALL PRIVILEGES ON DATABASE "product_db" TO product_user;
EOSQL