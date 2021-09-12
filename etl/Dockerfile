FROM maven:3.8.2-openjdk-11 AS MAVEN_TOOL_CHAIN
ENV HOME=/home/usr/app
RUN mkdir -p $HOME
WORKDIR $HOME
COPY pom.xml $HOME

# download all dependencies from MAVEN repository
RUN mvn --no-transfer-progress -B -f $HOME/pom.xml -s /usr/share/maven/ref/settings-docker.xml dependency:go-offline

# copy web application source code
COPY src $HOME/src/

# duplicate language properties bundle
RUN cp $HOME/src/main/resources/messages.properties $HOME/src/main/resources/messages_pt.properties
RUN cp $HOME/src/main/resources/messages.properties $HOME/src/main/resources/messages_br.properties

# compile and package web application
RUN mvn --no-transfer-progress -B -f $HOME/pom.xml -s /usr/share/maven/ref/settings-docker.xml package -DskipTests

FROM openjdk:11

# container creator
LABEL maintainer=gustavohbf@gmail.com

ENV HOME=/home/usr/app
RUN mkdir -p $HOME
WORKDIR $HOME

# copy the script file into the container
COPY setup.sh $HOME
RUN chmod +x $HOME/setup.sh

# deploy web application
COPY --from=MAVEN_TOOL_CHAIN /home/usr/app/target/cacao_etl.jar $HOME/cacao_etl.jar

# copy remaining webapp static files
COPY src/main/webapp $HOME/src/main/webapp

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
