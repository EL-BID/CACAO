#!/bin/sh

# HTTP (no SSL, should only be used behind reverse proxy HTTPS->HTTP)
java $WEB_JVM_OPTS -DLOG_DIR=/var/log -jar /home/usr/app/cacao_etl.jar --server.port=$PORT --kibana.host=$KIBANA_HOST --kibana.port=$KIBANA_PORT --kibana.endpoint=$KIBANA_ENDPOINT --es.host=$ES_HOST --es.port=$ES_PORT --mail.ssl.key-store=classpath:cert.p12 --mail.ssl.key-store-password=123456 --spring.config.additional-location=file:/app_config.properties --resource.monitor=true
