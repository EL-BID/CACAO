# CACAO

## Minimalist setup for DEVELOPMENT ENVIRONMENT

For running the web component locally at a development desktop with minimal setup, you should follow these steps:

1. Start ElasticSearch and Kibana with one of the following alternatives:

1a. Start a node of ElasticSearch (version 7.14.1) with docker-compose-dev.yml. Run "docker-compose -f docker-compose-dev.yml up --build -d".

OR

1b. Download ElasticSearch (version 7.14.1) from the official download site (https://www.elastic.co/pt/downloads/elasticsearch)
and also download Kibana (version 7.14.1) from the official download site (https://www.elastic.co/pt/downloads/kibana). The configuration file of ElasticSearch
may be the same as the provided from the download. The configuration file of Kibana (kibana.yml) must be changed in order to include the following lines:

    server.basePath: /kibana
    server.rewriteBasePath: true
    
Start both ElasticSearch and Kibana using the local startup files (e.g. for Windows platform, use \bin\elasticsearch.bat from ElasticSearch installation directory
for starting one node of ElasticSearch and use \bin\kibana.bat from Kibana installation directory for starting Kibana). 
Pay attention to the versions of kibana and elasticsearch, kibana may not work if they are from versions too apart.

2. Compile/build the ***CACAO Web project*** . If you are using an IDE such as Eclipse, the automatic build process should be enough. 

3. Run the Web application (org.idb.cacao.web.WebApplication) using developer application properties, referring to the property file using command line arguments. You may use the provided internal file 'dev.properties' like this:

> --spring.config.additional-location=classpath:dev.properties

4. Access the front page using your browser. Confirm/bypass the security warning related to the unsafe self-signed certificate being used with the developer configuration.

> `https://127.0.0.1:8888/`

5. For an initial empty database, use the following login credentials:

> Login: admin@admin
>
> Password: 123456

***Important:*** Please change the initial admin credentials as soon as possible, using the 'User' menu in CACAO web interface.

6. Compile/build the ***CACAO Validator project*** , just as you did with ***CACAO Web project*** .

7. Run the Validator application (org.idb.cacao.validator.Application) using a different port number, so that it does not conflict with the running Web Application in the same local host (both should be running simultaneously). Simply inform the following command line option:

> --server.port=8081

Please note the Web application should start before starting Validator application in a development environment. This is due to an additional component (KAFKA) that is part of the overall architecture. In a development environment there is an 'embedded' KAFKA broker being started alongside with the Web application. The Validator application requires this in order to work properly. 

8. Compile/build the ***CACAO ETL project*** , just as you did with ***CACAO Web project*** .

9. Run the ETL application (org.idb.cacao.etl.Application) using a different port number, so that it does not conflict with the running Web Application neither the Validator Application (all three should be running simultaneously). Simply inform the following command line option:

> --server.port=8082

Please note the Web application should start before starting ETL application in a development environment. As mentioned before, this is due to an additional component (KAFKA) that is part of the overall architecture. The ETL application requires this in order to work properly. There is no specific order regarding ETL and Validator applications. 

___

## Developing new CACAO modules

The CACAO infrastructure contains general-purpose modules and also specific modules.

One example of a specific module is the 'CACAO_ACCOUNT' module related to ACCOUNTING.

New modules may be developed and integrated into the CACAO infrastructure, sharing all the common application components.

The new module should implement the interface 'TemplateArchetype' defined at CACAO_API module. It's allowed to have multiple implementations of the same interface 'TemplateArchetype' in the same specific module.

CACAO use Java's Service Provider Interface (SPI) for discovery of 'TemplateArchetype' implementations. So, for this reason, the new module should also expose its implementations in static file inside 'META-INF/services' according to the Java SPI specification.

Whenever developing new modules, It's also necessary to include references to the new module at the following locations:

- Includes reference to the new module at the 'modules' session of 'pom.xml' at project's root directory.
- Add a new service entry at 'docker-compose.yml' intended to build and tag a new docker image related to this module (similar to 'plugin_account' declared in 'docker-compose.yml').
- Include references to this new service in 'depends_on' session of the following services declared at 'docker-compose.yml': web , validator and etl.
- Modify the 'Dockerfile' of 'etl', 'validator' and 'web' modules in order to include an additional 'COPY' line at the beginning of the file, similar to 'cacao_plugin_account'.
- Modify the 'pom.xml' of 'etl', 'validator' and 'web' modules in order to include an additional 'dependency' to the artifact produced by the module (similar to 'CACAO_ACCOUNT').

___

## How to setup REPOSITORY MANAGER

These are the steps for installing and configuring a REPOSITORY MANAGER at the same node where the Docker images are built. This is used as PROXY for MAVEN public repositories.

This is a recommended procedure for lowering the network bandwidth required for multiple changes in CACAO project during the development.

With a REPOSITORY MANAGER working as a PROXY for MAVEN public repository, the project's dependencies are cached locally (at the same server where the REPOSITORY MANAGER is running).

There are different types of REPOSITORY MANAGER that may be used for this purpose. Very briefly, these are the steps for installing and using SONATYPE NEXUS REPOSITORY OSS, which is free to use.

1. Create a volume for storing the NEXUS data (i.e. the cached repository files)

```
docker volume create nexus-data
```

2. Start a NEXUS server locally, exposing some port numbers that will be used whenever building docker images with Maven components in this project

```
docker run -d -p 8081:8081 -p 8082:8082 -p 8083:8083 -v nexus-data:/nexus-data --restart unless-stopped --name my-nexus sonatype/nexus3:3.34.1
```

3. Show the log entries from the running container.

```
docker logs --follow my-nexus
```

Wait until this:

```
Started Sonatype Nexus OSS 3.34.1-01
```

Press CTRL+C to abort waiting for LOG messages.

4. See the password for admin account that has been created for you (it will be necessary for initial Nexus setup)

```
docker exec -it my-nexus cat /nexus-data/admin.password
```

5. Configure NEXUS server using the user interface at port 8081. This requires a browser. If you are using a remote terminal without a browser, you may need configure something like 'tunneling through SSH', connecting the port 8081 from the server to the client machine running the terminal shell, so that you can open a browser at your local machine.

`http://localhost:8081`

6. Sign in at the user interface (see the 'Sign in' button at top right corner)

Inform 'admin' as login and inform the password you saw at step 4.

When prompted, inform new password for 'admin' account.

When prompted, confirm the option for 'anonymous access' to the repository (otherwise you will need to provide user credentials at each of the project modules).

___

## How to setup SONARQUBE 

Sonarqube is a open source platform for continuous inspection of code quality. It's used at development environment in order to check if the code complies to generally accepted programming standards.

It requires a 'server' related to Sonarqube and also some additional tools provided by Sonarqube platform that are used to 'scan' the source code. There are different ways to setup Solarqube platforms and there are different versions of it. Here we are going to show the quickest way possible to setup and use the 'Community Edition'.

1. For the 'server' part, it's possible to start quickly with a docker image. If you want to run the latest available image version and want to make it accessible at localhost using the default port number 9000, just type in:

```
docker run -d --name sonarqube -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true -p 9000:9000 sonarqube:latest
```

2. Sonarqube provides a web user interface and requires user authentication. For your first access as 'admin' user you will need to provide a new password. So log in to the user interface, like this:

`http://127.0.0.1:9000/`

3. When prompted for credentials, type in the initial 'admin/admin' (user: admin, password: admin) account. After confirmation, you have to provide a new password for this 'admin' account. Just proceed as instructed.

4. After changing the password, the user interface may welcome you with some hints about projects, but you can just skip these steps and jump into the 'accounts page' in order to create an 'access token' that will be required by Maven later. Just go to the 'Accounts' / 'Security' page, or follow this link:

`http://127.0.0.1:9000/account/security/`

5. Generate a new token. Choose a token name (e.g.: "Maven build") and press 'generate'

6. Copy the generated token (it's very important you do it now, because the token will not be presented again later)

7. Edit your Maven 'settings.xml' file (for Windows users, locate this file at your user home directory, subdirectory '.m2').

8. If you already have a settings.xml file, include the following lines:
   
```
<pluginGroups>
	<pluginGroup>org.sonarsource.scanner.maven</pluginGroup>
</pluginGroups>
<profiles>
	<profile>
		<id>sonar</id>
		<activation>
			<activeByDefault>true</activeByDefault>
		</activation>
		<properties>
			<sonar.host.url>http://127.0.0.1:9000</sonar.host.url>
			<sonar.login>  ... paste your Sonarqube access token here ... </sonar.login>
		</properties>
	</profile>
</profiles>
```

If there's no settings.xml file, you should create at .m2 directory

```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">

 <localRepository>  ... paste your maven local repository path here ... </localRepository>
 <interactiveMode>true</interactiveMode>
 <offline>false</offline>

 <pluginGroups>
 	<pluginGroup>org.sonarsource.scanner.maven</pluginGroup>
 </pluginGroups>

 
<profiles>
	<profile>
		<id>sonar</id>
		<activation>
			<activeByDefault>true</activeByDefault>
		</activation>
		<properties>
			<sonar.host.url>http://127.0.0.1:9000</sonar.host.url>
			<sonar.login>  ... paste your Sonarqube access token here ... </sonar.login>
		</properties>
	</profile>
</profiles>

  <activeProfiles></activeProfiles>
  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <artifactId>maven-resources-plugin</artifactId>
          <version>  ... paste your installed maven version here ... </version>
        </plugin>           
      </plugins>
    </pluginManagement>   
</build>
</settings>
```


9. If you didn't do it yet, build CACAO using the following Maven command line (standing at the project root directory):

```
mvn install
```

10. Run the following Maven command line for starting the code scanning using Sonarqube (you need at least Java 11 for this - older versions of Java won't work)

```
mvn sonar:sonar
```

Note: it's also possible to run both commands at once, like this:

```
mvn install sonar:sonar
```
 
11. The procedure may take some time to complete. After all the work, check for error messages (if any). If the procedure runs fine, check the results at the web user interface (the URL will be displayed as result of the previous command).
 
___

## How to DEPLOY

___

## Additional information

### Setup minimal firewall rules

The minimal rule set for a firewall running CACAO should deny all incoming connections and allow all outgoing connections. Afterwards, incoming connections with the SSH, HTTPS and HTTP should be allowed.

For this minimal setup, the Uncomplicated Firewall (ufw) will be used. The following commands install ufw and configure it.

```
sudo yum install epel-release -y
sudo yum install --enablerepo="epel" ufw -y
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow https
sudo ufw allow http
sudo ufw enable
sudo systemctl enable ufw
```

For the Amazon Machine Image (AMI) linux, in the previous list of commands the first one:

```
sudo yum install epel-release -y
```

must be replaced for:

```
sudo amazon-linux-extras install epel
```

### Setup EC2 instance at AWS (Amazon)

1. Hardware requirements

CPU: 4		Memory: 16 GB		Arch: x86_64	SO: Amazon Linux 2	Disk: 60 GB

Suggested instance type: m4.xlarge + EBS volume

2. Install GIT

sudo yum update -y

sudo yum install git

3. Install docker

sudo yum update -y

sudo yum install docker -y

sudo service docker start

sudo usermod -a -G docker $USER

sudo systemctl enable docker

<logout and and login again>

sudo curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

sudo chmod +x /usr/local/bin/docker-compose

sudo ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose

echo 'vm.max_map_count=262144' | sudo tee -a /etc/sysctl.conf

sudo sysctl -w vm.max_map_count=262144

4. Clone GIT Repository

git clone https://github.com/gustavohbf/cacao.git

cd cacao

5. BUILD with Docker Compose

docker-compose build

6. RUN with Docker Compose

docker-compose up -d


### Fix eventual issues with OAUTH (e.g. Login with Google) regarding 'An error occurred while attempting to decode the Jwt: The ID Token contains invalid claims:'

Usually this problem is related to failure in synchronizing the system clock with external server (e.g. Google's) clocks.

In order to check this, execute:

    systemctl status chronyd
    
Look for some error message regarding this service. E.g.: 'Active: failed (Result: exit-code) ' or '(code=exited, status=1/FAILURE)'

In order to fix this, execute:

    sudo systemctl enable --now chronyd
    

___

## Configure Role Based User Authentication (RBAC) for ElasticSearch and Kibana

1. Create self signed certificates for every component in the stack

    docker cp conf/instance.yml es01:/usr/share/elasticsearch/config/instance.yml
    docker exec -it es01 /usr/share/elasticsearch/bin/elasticsearch-certutil cert --keep-ca-key --pem --in /usr/share/elasticsearch/config/instance.yml --out /usr/share/elasticsearch/CERT/certs.zip

Confirm with 'Y' if prompted to create 'CERT' directory.

2. Copy certificate files to local

    docker cp es01:/usr/share/elasticsearch/CERT/certs.zip conf/certs.zip
    unzip conf/certs.zip -d conf
    
If 'unzip' is not recognized as a valid command, install it first (e.g.: yum install unzip)

3. Copy certificate files to each node of ElasticSearch and Kibana

    chmod u+x conf/copy_certs.sh
    conf/copy_certs.sh

The previous command should finish without error messages. It might eventually fail to copy certificates related to Kibana component with an error message like this:

    Error: No such container:path: kibana:/usr/share/kibana/config/certs

In the case of failure copying certificate files to Kibana, please proceed with the corrective steps provided in this box:

```
Steps for dealing with the repeating error ‘No such container:path: kibana:/usr/share/kibana/config/certs’

1 – Stop the failing ‘Kibana’ component
    docker stop kibana

2 – Start a new temporary docker container mounting the volume that will be used by the Kibana component

    docker run -it --name temp-kibana --rm -v cacao_kibana-conf:/dest -w /dest alpine /bin/sh

3 – Create the necessary directory inside the mounted volume
    docker exec -it temp-kibana mkdir -p /dest/certs

4 – Copy all the missing files into the directory
    docker cp conf/ca/ca.crt temp-kibana:/dest/certs/ca.crt
    docker cp conf/ca/ca.key temp-kibana:/dest/certs/ca.key
    docker cp conf/kibana/kibana.crt temp-kibana:/dest/certs/kibana.crt
    docker cp conf/kibana/kibana.key temp-kibana:/dest/certs/kibana.key

5 – Stop the temporary docker container
    docker stop temp-kibana

6 – Restart the ‘Kibana’ component
    docker up -d kibana
```


4. Stop running components
    docker-compose stop web etl validator kibana es01 es02 es03

5. Copy 'docker-compose.override.ssl.yml' to 'docker-compose.override.yml'
    cp docker-compose.override.ssl.yml docker-compose.override.yml 

***WARNING***

If you are using a different version of 'docker-compose.yml', you may need to check the
contents of 'docker-compose.override.ssl.yml' and see if they match you current
configuration (e.g. check the services names and the number of them)

6. Start Elastic nodes
    docker-compose up -d es01 es02 es03

7. Access shell inside ElasticSearch container
    docker exec -it es01 /bin/bash

8. Generates random passwords for ElasticSearch components
    /usr/share/elasticsearch/bin/elasticsearch-setup-passwords auto -u "https://es01:9200"
    
When prompted, confirm with 'y' and ENTER

***VERY IMPORTANT!!!*** The previous command will print all the passwords at the console. ***COPY AND PASTE*** these random passwords to a safe notebook to continue with the configuration. After all the configurations are done, you SHOULD delete these annotations.
    
Take note of login and passwords for all user accounts that were generated (most important ones: 'elastic' and 'kibana')

9. Exit shell from es01

10. Add to .env file at the host the password that was generated for 'kibana' user. Change the {password} below with the actual password that has been generated
    echo KIBANA_PASSWORD={password} >> .env

11. Add to .env file at the host the password that was generated for 'elastic' user. Change the {password} below with the actual password that has been generated
    echo ELASTIC_PASSWORD={password} >> .env

12. Test arbitrary HTTP call to the ElasticSearch.
    docker exec --env-file .env -it es01 /bin/bash -c 'curl -k https://es01:9200 -u kibana:$KIBANA_PASSWORD'

It should respond with a JSON content with some information, including something like this: "tagline" : "You Know, for Search"
 
13. Start Kibana
    docker-compose up -d kibana
 
14. Check LOG entries for errors (<CTRL+C> to terminate LOG after a while)
    docker exec -it kibana tail -f /usr/share/kibana/logs/kibana.log
    
15. Test arbitrary HTTP call to the Kibana.
    docker exec --env-file .env -it kibana /bin/bash -c 'curl -k https://kibana:5601/kibana/api/spaces/space -u elastic:$ELASTIC_PASSWORD'
    
It should respond with a JSON content with some information about the Kibana default 'space', among others.
    
16. Include all permissions to the Application
    echo 'es.user=elastic' | tee -a app_config_web app_config_etl app_config_validator
    
    echo 'es.password=${ELASTIC_PASSWORD}' | tee -a app_config_web app_config_etl app_config_validator
    
    echo 'es.ssl=true' | tee -a app_config_web app_config_etl app_config_validator

    echo 'es.ssl.verifyhost=false' | tee -a app_config_web app_config_etl app_config_validator
    
17. Start Application nodes
    docker-compose up -d web etl validator
    
18. Check LOG entries for errors (<CTRL+C> to terminate LOG after a while)
    docker logs --follow web

19. Start Proxy node
    docker-compose up -d proxy
    
20. Test access to CACAO using your browser

___

## Generating random (sample) data

In order to test the application without actual data, you may use some CACAO features for randomly generating data according to built-in templates or any other custom templates.

These features must be performed using a SYSTEM ADMINISTRATOR profile. They should NOT be used under a PRODUCTION environment, because they may replace existing actual data. 

Every command line shown here must be executed using the 'System operations' menu. Just type in the command line using the CACAO command prompt and press ENTER. Check the result panel for the produced messages.

1. Deleting all previous data

If you want to start the CACAO fresh new, type in this command. It will delete all previous templates, validated document and published data.

    delete -a
    
There are other forms of the command line 'delete' that may be used for deleting only part of data. Use 'help delete' for more information about these options.

2. Generating built-in templates

If you want to create 'default templates' according to built-in archetypes, type in this command.

    samples -t
    
The previous command line will create a couple of 'templates' and all the needed 'domain tables'.

3. Generating documents with random data

If you want to create some 'documents' with random data simulating files being uploaded by different taxpayers, use the command 'samples' with the command option '--doc', followed by the name of the template for which you want to generate files. Usually you will also inform the number of documents to generate using the command line option '--limit_docs' and an initial 'random seed' using the command line option '--seed'.

For example, the following command line will generate 10 different documents conforming to the built-in template 'General Ledger' using the text 'TEST' as a seed (any word may be used as a 'seed').

    samples --docs "General Ledger" --limit_docs 10 --seed "TEST"
    
If you execute the same command line with the same seed, it will generate documents with the exact same contents as before. If you use a different seed, the command will produce different contents. The 'seed' is important for producing the same contents at different environments. It's also important to produce documents with different template using the same 'seed' in order to garantee consistence accross different templates.

For example, the following command line will generate 10 different documents conforming to the built-in template 'Chart of Accounts' using the same text 'TEST' as a seed, so that these documents will have consistency with the previously generated documents of 'General Ledger' template.

    samples --docs "Chart of Accounts" --limit_docs 10 --seed "TEST"
 
For completeness, the accounting data should also include 'Opening Balance'. The following command line will generate 10 different documents conforming to the built-in template 'Opening Balance' using the same text 'TEST' as a seed.

    samples --docs "Opening Balance" --limit_docs 10 --seed "TEST"
    
In each of the previous commands (those ones starting with 'samples --docs') you may include the '-bg' parameter to avoid waiting for conclusion. For example:
     
    samples --docs "Opening Balance" --limit_docs 10 --seed "TEST" -bg
    
The previous command will start creating the 10 documents of template 'Opening Balance' in background. So it will be possible to execute other commands to start creating other kinds of documents at the same time. 
    
Just to recap, the following command lines (each one entered alone) will start over a new environment will random data ready to be used.

    delete -a
    samples -t
    samples --docs "Chart Of Accounts" --limit_docs 10 --seed "TEST" -bg
    samples --docs "General Ledger" --limit_docs 10 --seed "TEST" -bg
    samples --docs "Opening Balance" --limit_docs 10 --seed "TEST" -bg
    
You may generate different amounts of documents and may use different texts as 'seed' for producing different data. Use the command 'help samples' for more information about the command line options.

    
