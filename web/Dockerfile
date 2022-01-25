FROM cacao_api AS MAVEN_TOOL_CHAIN

# copy plugins compiled at other Docker images to local repository
COPY --from=cacao_plugin_account /root/.m2/repository/org/idb/cacao/CACAO_ACCOUNT /root/.m2/repository/org/idb/cacao/CACAO_ACCOUNT

# copy application specific configuration files
COPY web/pom.xml $HOME

# download all dependencies from MAVEN repository
RUN mvn --no-transfer-progress -B -f $HOME/pom.xml ${MVN_ARGS} dependency:go-offline

# copy web application source code
COPY web/src $HOME/src/

# duplicate language properties bundle
RUN cp $HOME/src/main/resources/messages.properties $HOME/src/main/resources/messages_en.properties

# compile and package web application
RUN mvn --no-transfer-progress -B -f $HOME/pom.xml ${MVN_ARGS} package -DskipTests

FROM openjdk:11

# container creator
LABEL maintainer=gustavohbf@gmail.com

ENV HOME=/home/usr/app
RUN mkdir -p $HOME
WORKDIR $HOME

# copy the script file into the container
COPY web/setup.sh $HOME
RUN chmod +x $HOME/setup.sh

# deploy web application
COPY --from=MAVEN_TOOL_CHAIN /home/usr/app/target/cacao_web.jar $HOME/cacao_web.jar

ENV CATALINA_HOME=$HOME

ENV ES_HOST=es01
ENV ES_PORT=9200

ENV KIBANA_HOST=kibana
ENV KIBANA_PORT=5601
ENV KIBANA_ENDPOINT=/kibana

# List of KAFKA Brokers (host:port)
ENV KAFKA_BROKERS=kafka:9092

# Eureka server for auto-discovery of services (host:port)
ENV EUREKA_SERVER=eureka:8761

ENV WEB_JVM_OPTS="-Xms512m -Xmx512m"

# additional configuration and start application
CMD ["./setup.sh"]

# expose the Tomcat port
EXPOSE $PORT
