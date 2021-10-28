FROM maven:3.8.2-openjdk-11 AS MAVEN_TOOL_CHAIN
ENV HOME=/home/usr/app
RUN mkdir -p $HOME
WORKDIR $HOME
COPY etl/settings-docker.xml /usr/share/maven/ref/settings-docker.xml

# copy common API source code
ENV HOME_API=/home/usr/api
RUN mkdir -p $HOME_API
COPY api/pom.xml $HOME_API
COPY api/src $HOME_API/src

# copy application specific configuration files
COPY etl/pom.xml $HOME

# download all dependencies from MAVEN repository
ARG MVN_ARGS=${USE_PROXY:+'-s /usr/share/maven/ref/settings-docker.xml'}
RUN mvn --no-transfer-progress -B -f $HOME/pom.xml ${MVN_ARGS} dependency:go-offline -DexcludeArtifactIds=CACAO_API

# copy ETL application source code
COPY etl/src $HOME/src/

# duplicate language properties bundle
RUN cp $HOME/src/main/resources/messages.properties $HOME/src/main/resources/messages_en.properties

# compile, package and install common API
RUN mvn --no-transfer-progress -B -f $HOME_API/pom.xml ${MVN_ARGS} install -DskipTests

# compile and package web application
RUN mvn --no-transfer-progress -B -f $HOME/pom.xml ${MVN_ARGS} package -DskipTests

FROM openjdk:11

# container creator
LABEL maintainer=gustavohbf@gmail.com

ENV HOME=/home/usr/app
RUN mkdir -p $HOME
WORKDIR $HOME

# copy the script file into the container
COPY etl/setup.sh $HOME
RUN chmod +x $HOME/setup.sh

# deploy web application
COPY --from=MAVEN_TOOL_CHAIN /home/usr/app/target/cacao_etl.jar $HOME/cacao_etl.jar

ENV CATALINA_HOME=$HOME

ENV ES_HOST=es01
ENV ES_PORT=9200

ENV KIBANA_HOST=kibana
ENV KIBANA_PORT=5601
ENV KIBANA_ENDPOINT=/kibana

ENV WEB_JVM_OPTS="-Xms512m -Xmx512m"

# additional configuration and start application
CMD ["./setup.sh"]

# expose the Tomcat port
EXPOSE $PORT
