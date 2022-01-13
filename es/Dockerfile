# https://github.com/elastic/elasticsearch/tree/v7.14.1/distribution/docker
FROM elasticsearch:7.14.1

# container creator
LABEL maintainer=gustavohbf@gmail.com

# The original elasticsearch image version 7.14.1 released on Sep, 2021 refers
# to a version of LOG4J-2 full of vulnerabilities (e.g.: CVE-2021-44832,
# CVE-2021-45046, CVE-2021-45105 and more)
# So we need to replace it with a newer version of LOG4J-2
RUN rm /usr/share/elasticsearch/lib/log4j-core-2.11.1.jar ; \\
    rm /usr/share/elasticsearch/lib/log4j-api-2.11.1.jar
RUN curl --retry 10 --show-error --location --remote-name --output-dir /usr/share/elasticsearch/lib https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.17.1/log4j-core-2.17.1.jar
RUN curl --retry 10 --show-error --location --remote-name --output-dir /usr/share/elasticsearch/lib https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/2.17.1/log4j-api-2.17.1.jar

ENV PORT=9200
ENV PORT2=9300

# copy the configuration file into the container
COPY log4j2.properties /usr/share/elasticsearch/config
COPY setup.sh .
RUN chmod +x ./setup.sh

# additional configuration and start application
CMD ["./setup.sh"]

# expose the default Elasticsearch port
EXPOSE $PORT $PORT2
