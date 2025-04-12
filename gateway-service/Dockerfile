#FROM nginx:alpine
FROM nginx:latest

#COPY ,/nginxnginx.conf.template /etc/nginx/nginx.conf.template

#COPY ./nginx/templates/ /etc/nginx/templates
COPY ./ssl/ /etc/nginx/ssl/
#COPY ./test.txt /etc/nginx/test.txt

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]
