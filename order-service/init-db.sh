#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --U postgres -d postgres <<-EOSQL
    CREATE USER order_user WITH PASSWORD 'order_password';
    CREATE DATABASE "order_db" WITH OWNER order_user;
    GRANT ALL PRIVILEGES ON DATABASE "order_db" TO order_user;
EOSQL