FROM elasticsearch:7.16.1

# container creator
LABEL maintainer=gustavohbf@gmail.com

ENV PORT=9200
ENV PORT2=9300

# copy the configuration file into the container
COPY log4j2.properties /usr/share/elasticsearch/config
COPY setup.sh .
RUN chmod +x ./setup.sh

# additional configuration e start application
CMD ["./setup.sh"]

# expose the default Elasticsearch port
EXPOSE $PORT $PORT2
