#FROM nginx:alpine
FROM nginx:latest

#COPY ./nginx/nginx.conf.template /etc/nginx/nginx.conf.template

COPY ./nginx/nginx.conf /etc/nginx.conf
COPY ./ssl/ /etc/nginx/ssl/

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]
