#FROM nginx:alpine
FROM nginx:latest

#COPY ./nginx/nginx.conf.template /etc/nginx/nginx.conf.template

COPY ./nginx/template/nginx.conf /etc/template/nginx.conf
COPY ./ssl/ /etc/nginx/ssl/

EXPOSE 80 443

RUN cp -f /etc/template/nginx.conf /etc/nginx.conf
CMD ["nginx", "-g", "daemon off;"]
