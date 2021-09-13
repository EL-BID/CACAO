FROM linuxserver/swag

# container creator
LABEL maintainer=gustavohbf@gmail.com

RUN mkdir -p /config/nginx/site-confs

# Remove NGINX version from HTTP responses (for security reason)
# RUN sed -i 's/# server_tokens off/ server_tokens off/' /config/nginx/nginx.conf

COPY config/default /config/nginx/site-confs/default
COPY config/*.sub* /config/nginx/proxy-confs/
