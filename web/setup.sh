#!/bin/sh

# HTTP (no SSL, should only be used behind reverse proxy HTTPS->HTTP)
java $WEB_JVM_OPTS -DLOG_DIR=/var/log -jar /home/usr/app/cacao_web.jar --server.port=$PORT --kibana.host=$KIBANA_HOST --kibana.port=$KIBANA_PORT --kibana.endpoint=$KIBANA_ENDPOINT --es.host=$ES_HOST --es.port=$ES_PORT --spring.cloud.stream.kafka.binder.brokers=$KAFKA_BROKERS --mail.ssl.key-store=file:/home/usr/app/.cacao/cert.p12 --mail.ssl.key-store-password=123456 --mail.ssl.enabled=true --eureka.client.serviceUrl.defaultZone=http://$EUREKA_SERVER/eureka/ --spring.config.additional-location=file:/app_config.properties --resource.monitor=true
