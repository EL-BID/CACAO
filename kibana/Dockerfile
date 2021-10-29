FROM kibana:7.14.1

# container creator
LABEL maintainer=gustavohbf@gmail.com

# copy the configuration file into the container
COPY --chown=kibana:kibana kibana.yml /usr/share/kibana/config

RUN mkdir -p /usr/share/kibana/logs

# ElasticSearch default port number
ENV ESPORT=9200

ENV SERVER_NAME="my-kibana"
ENV SSL_ENABLED=false
ENV ES_HOSTS=["http://es01:${ESPORT}"]
ENV KIBANA_USERNAME=
ENV KIBANA_PASSWORD=

# expose the default Kibana port
EXPOSE $PORT
