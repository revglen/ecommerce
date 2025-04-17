#FROM nginx:alpine
FROM nginx:latest

#COPY ./nginx/nginx.conf.template /etc/nginx/nginx.conf.template
RUN mkdir -p /etc/nginx/templates
COPY ./nginx/templates/nginx.conf /etc/templates/nginx.conf
RUN cp -f /etc/template/nginx.conf /etc/nginx.conf

COPY ./ssl/ /etc/nginx/ssl/

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]
