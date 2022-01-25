FROM maven:3.8.2-openjdk-11 AS MAVEN_TOOL_CHAIN

ENV HOME_EUREKA=/home/usr/eureka
RUN mkdir -p $HOME_EUREKA
WORKDIR $HOME_EUREKA

COPY eureka/settings-docker.xml /usr/share/maven/ref/settings-docker.xml

ARG MVN_ARGS=${USE_PROXY:+'-s /usr/share/maven/ref/settings-docker.xml'}

# copy EUREKA SERVER source code
COPY eureka/pom.xml $HOME_EUREKA
COPY eureka/src $HOME_EUREKA/src

# compile, package and install EUREKA SERVER
RUN mvn --no-transfer-progress -B -f $HOME_EUREKA/pom.xml ${MVN_ARGS} install -DskipTests

FROM openjdk:11

# container creator
LABEL maintainer=gustavohbf@gmail.com

ENV HOME=/home/usr/app
RUN mkdir -p $HOME
WORKDIR $HOME

# copy the script file into the container
COPY eureka/setup.sh $HOME
RUN chmod +x $HOME/setup.sh

# deploy web application
COPY --from=MAVEN_TOOL_CHAIN /home/usr/eureka/target/cacao_eureka.jar $HOME/cacao_eureka.jar

ENV CATALINA_HOME=$HOME

ENV WEB_JVM_OPTS="-Xmx512m"

# additional configuration and start application
CMD ["./setup.sh"]

# expose the Tomcat port
EXPOSE $PORT
