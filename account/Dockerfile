FROM cacao_api AS MAVEN_TOOL_CHAIN

# copy application specific configuration files
COPY account/pom.xml $HOME

# download all dependencies from MAVEN repository
RUN mvn --no-transfer-progress -B -f $HOME/pom.xml ${MVN_ARGS} dependency:go-offline

# copy account plugin source code
COPY account/src $HOME/src/

# compile, package and install account plugin
RUN mvn --no-transfer-progress -B -f $HOME/pom.xml ${MVN_ARGS} install -DskipTests
