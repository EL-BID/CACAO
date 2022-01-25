#!/bin/sh

# HTTP (no SSL, should only be used behind reverse proxy HTTPS->HTTP)
java $WEB_JVM_OPTS -DLOG_DIR=/var/log -jar /home/usr/app/cacao_eureka.jar --server.port=$PORT
