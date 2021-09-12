FROM kibana:7.14.1

# container creator
LABEL maintainer=gustavohbf@gmail.com

# copy the configuration file into the container
COPY --chown=kibana:kibana kibana.yml /usr/share/kibana/config

RUN mkdir -p /usr/share/kibana/logs

# ElasticSearch default port number
ENV ESPORT=9200

# expose the default Kibana port
EXPOSE $PORT
