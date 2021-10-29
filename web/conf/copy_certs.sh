#!/bin/bash
docker cp conf/ca/ca.crt es01:/usr/share/elasticsearch/config/certs/ca.crt
docker cp conf/ca/ca.key es01:/usr/share/elasticsearch/config/certs/ca.key
docker cp conf/es01/es01.crt es01:/usr/share/elasticsearch/config/certs/es01.crt
docker cp conf/es01/es01.key es01:/usr/share/elasticsearch/config/certs/es01.key

docker exec -it es02 mkdir -p /usr/share/elasticsearch/config/certs
docker cp conf/ca/ca.crt es02:/usr/share/elasticsearch/config/certs/ca.crt
docker cp conf/ca/ca.key es02:/usr/share/elasticsearch/config/certs/ca.key
docker cp conf/es02/es02.crt es02:/usr/share/elasticsearch/config/certs/es02.crt
docker cp conf/es02/es02.key es02:/usr/share/elasticsearch/config/certs/es02.key

docker exec -it es03 mkdir -p /usr/share/elasticsearch/config/certs
docker cp conf/ca/ca.crt es03:/usr/share/elasticsearch/config/certs/ca.crt
docker cp conf/ca/ca.key es03:/usr/share/elasticsearch/config/certs/ca.key
docker cp conf/es03/es03.crt es03:/usr/share/elasticsearch/config/certs/es03.crt
docker cp conf/es03/es03.key es03:/usr/share/elasticsearch/config/certs/es03.key

docker exec -it kibana mkdir -p /usr/share/kibana/config/certs
docker cp conf/ca/ca.crt kibana:/usr/share/kibana/config/certs/ca.crt
docker cp conf/ca/ca.key kibana:/usr/share/kibana/config/certs/ca.key
docker cp conf/kibana/kibana.crt kibana:/usr/share/kibana/config/certs/kibana.crt
docker cp conf/kibana/kibana.key kibana:/usr/share/kibana/config/certs/kibana.key
