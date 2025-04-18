CREATE USER order_user WITH PASSWORD 'order_password';
CREATE DATABASE order_db OWNER order_user;
GRANT ALL PRIVILEGES ON DATABASE order_db TO order_user;