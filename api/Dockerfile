FROM maven:3.8.2-openjdk-11

ENV HOME=/home/usr/app
RUN mkdir -p $HOME
WORKDIR $HOME
COPY api/settings-docker.xml /usr/share/maven/ref/settings-docker.xml

ARG MVN_ARGS=${USE_PROXY:+'-s /usr/share/maven/ref/settings-docker.xml'}

# copy parent POM
COPY pom.xml /home/usr

# install parent POM
RUN mvn --non-recursive -f /home/usr/pom.xml ${MVN_ARGS} install

# copy common API source code
ENV HOME_API=/home/usr/api
RUN mkdir -p $HOME_API
COPY api/pom.xml $HOME_API
COPY api/src $HOME_API/src

# compile, package and install common API
RUN mvn --no-transfer-progress -B -f $HOME_API/pom.xml ${MVN_ARGS} install -DskipTests

# copy source code for shared TEST ARTIFACTS
ENV HOME_MOCK_ES=/home/usr/mock_es
RUN mkdir -p $HOME_MOCK_ES
COPY mock_es/pom.xml $HOME_MOCK_ES
COPY mock_es/src $HOME_MOCK_ES/src

# compile, package and install shared TEST ARTIFACTS
RUN mvn --no-transfer-progress -B -f $HOME_MOCK_ES/pom.xml ${MVN_ARGS} install -DskipTests
