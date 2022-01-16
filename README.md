# CACAO

## What is CACAO

Sistema para Consulta y Almacenamiento de dados Contables y Apoyo Organizativo

___

## Minimalist setup for DEVELOPMENT 

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
for starting one node of ElasticSearch and use \bin\kibana.bat from Kibana installation directory for starting Kibana)

2. Compile/build the ***CACAO Web project*** . If you are using an IDE such as Eclipse, the automatic build should be enough. 

3. Run the application using developer application properties, referring to the property file using command line arguments. You may use the internal file 'dev.properties' like this:

> --spring.config.additional-location=classpath:dev.properties

4. Access the front page using your browser. Confirm/bypass the security warning related to the unsafe self-signed certificate being used with the developer configuration.

> https://127.0.0.1:8888/

5. For a initial empty database, use the following login credentials:

> Login: admin@admin
>
> Password: 123456

___

## How to BUILD

For full-stack deployment, use DOCKER-COMPOSE for building all CACAO modules.

    docker-compose build
    
If the application has already been deployed, use this command in order to build and update only the modified CACAO modules:

    docker-compose up --build -d
    
For building specific modules (for example, using an IDE), use MAVEN standing at the specific module.

    mvn install

___

## Developing new CACAO modules

The CACAO infrastructure contains general-purpose modules and also specific modules.

One example of a specific module is the 'CACAO_ACCOUNT' module related to ACCOUNTING.

New modules may be developed and integrated into the CACAO infrastructure, sharing all the common application components.

The new module should implement the interface 'TemplateArchetype' defined at CACAO_API. It's allowed to have multiple implementations of the same interface 'TemplateArchetype' in the same specific module.

CACAO use Java's Service Provider Interface for discovery of 'TemplateArchetype' implementations. So, for this reason, the new module should also give its implementations in static file inside 'META-INF/services' according to the Java SPI specification.

It's also necessary to include references to the new module at the following locations:

- includes referente to the new module at the 'modules' session of 'pom.xml' at project's root directory
- add a new service entry at 'docker-compose.yml' intended to build and tag a new docker image related to this module (similar to 'plugin_account' declared in 'docker-compose.yml')
- include references to this new service in 'depends_on' session of the following services declared at 'docker-compose.yml': web , validator and etl
- modify the 'Dockerfile' of 'etl', 'validator' and 'web' modules in order to include an additional 'COPY' line at the beggining of the file, similar to 'cacao_plugin_account'
- modify the 'pom.xml' of 'etl', 'validator' and 'web' modules in order to include an additional 'dependency' to the artifact produced by the module (similar to 'CACAO_ACCOUNT')

___

## How to setup REPOSITORY MANAGER

These are the steps for installing and configuring a REPOSITORY MANAGER at the same node where the Docker images are built. This is used as PROXY for MAVEN public repositories.

This is important for decreasing the network bandwidth required for building the Maven projects repeatedly over and over again, with changes to dependencies. In case there is no REPOSITORY MANAGER configured, for each BUILD after any changes in POM file will require that every file from every dependency be fetched again from public repositories, what is time consuming.

With a REPOSITORY MANAGER working as a PROXY for MAVEN public repository, these dependencies are cached locally (at the server where the REPOSITORY MANAGER is running).

There are different types of REPOSITORY MANAGER that may be used for this purpose. Here are the steps for using SONATYPE NEXUS REPOSITORY OSS that is free to use.

1. Create a volume for storing the NEXUS data (i.e. the cached repository files)

```
docker volume create nexus-data
```

2. Start a NEXUS server locally, exposing some port numbers that will be used whenever building docker images with Maven components in this project

```
docker run -d -p 8081:8081 -p 8082:8082 -p 8083:8083 -v nexus-data:/nexus-data --restart unless-stopped --name my-nexus sonatype/nexus3:3.34.1
```

3. Show de logs from the running container.

```
docker logs --follow my-nexus
```

Wait until this:

```
Started Sonatype Nexus OSS 3.34.1-01
```

Press CTRL+C to abort waiting for LOG messages.

4. See the password for admin account (it will be necessary for initial Nexus setup)

```
docker exec -it my-nexus cat /nexus-data/admin.password
```

5. Configure NEXUS server using the user interface at port 8081. This requires a browser. If you are using a remote terminal without a browser, you may need configure something like 'tunneling through SSH' the port 8081 from the server to the client machine runnig the terminal shell, so that you can open a browser at your localmachine.

http://localhost:8081

6. Sign in at the user interface (see the 'Sign in' button at top right corner)

Inform 'admin' as login and inform the password you saw at step 4.

When prompted, inform new password for 'admin' account.

When prompted, confirm the option for 'anonymous access' to the repository (otherwise you will need to provide user credentials at each of the project modules).
  
___

## How to Setup Google Authentication (Login with Google)

- Create a Google account and create a Project (see https://cloud.google.com/resource-manager/docs/creating-managing-projects)
- Configure a 'Consent Screen' (https://console.cloud.google.com/apis/credentials/consent)
  - When prompted to choose between 'Internal' or 'Public' application, choose 'Public' in order to allow users outside your organization to access the website
  - Inform the product name (e.g. CACAO) and e-mail
  - Upload the application logo (image file with size lesser than 1 MB)
  - For start page, inform the URI within the domain name (e.g.: https://cacaoidb.duckdns.org/)
  - For Privacy Policy page, inform the following URI (change only domain name if necessary, keep the rest of this URI): https://cacaoidb.duckdns.org/privacy
  - For Terms of Use page, inform the following URI (change only domain name if necessary, keep the rest of this URI): https://cacaoidb.duckdns.org/terms
  - For Authorized Domain Names, inform the domain name (e.g. cacaoidb.duckdns.org)
  - For developer contact, inform your e-mail
  - Click 'Add Scope'
  - Choose the following scopes from the list of scopes:
    - .../auth/userinfo.email		(See your primary Google Account email address)
    - .../auth/userinfo.profile		(See your personal info, including any personal info you've made publicly available)
    - 	openid						(Associate you with your personal info on Google)
  - Add some initial 'test users' for testing purposes
- Create OAuth2 credentials in Credentials Page (for more information, see https://developers.google.com/identity/protocols/oauth2/openid-connect and https://console.developers.google.com/apis/credentials)
  - Go to this link and create a new OAuth2 token: https://console.developers.google.com/apis/credentials
  - Choose a name for this OAuth token (e.g. CACAO)
  - For Javascript Authorized Sources, informs the following URI (change only domain name if necessary, keep the rest of this URI): https://cacaoidb.duckdns.org
  - If also using for development environment, also include the following URL for authorized JavaScript source: https://127.0.0.1:8888
  - Set a redirect UI following this pattern (change only domain name if necessary, keep the rest of this URI): https://cacaoidb.duckdns.org/login/oauth2/code/google
  - If also using for development environment, also include the following URL for authorized redirections: https://127.0.0.1:8888/login/oauth2/code/google
  - Take note of these information and keep secret (do not publish anywhere):
    - Application ID (client)
    - Secret
  - You must inform these information in the configuration file 'app_config', at the deployment environment, accessible only by root user
    - spring.security.oauth2.client.registration.google.client-id=<copy here you application ID from Google>
    - spring.security.oauth2.client.registration.google.client-secret=<copy here your secret from Google>
- Publish the application (from 'Testing' to 'Production')
  - Access the 'Consent Screen' configuration page (https://console.cloud.google.com/apis/credentials/consent)
  - Press the 'Publish Application' button
  
___
  
## How to Setup Microsoft Azure Authentication

- Create an account at Microsoft Azure and create a Tenant (see https://docs.microsoft.com/pt-br/azure/active-directory/fundamentals/active-directory-access-create-new-tenant)
  - The page for tenant creation is here: https://portal.azure.com/#create/hub (at this page, search for 'Azure Active Directory')
  - You will provide an 'organization name', an initial 'domain name' (the application website) and your country
- Create OAuth2 credentials registering new app (see https://docs.microsoft.com/pt-br/azure/active-directory/develop/quickstart-register-app)
  - The page for application creation is here: https://go.microsoft.com/fwlink/?linkid=2083908
  - For 'type of account', choose 'Accounts in any tenant (multitenant) including personal accounts (e.g. Skype, Xbox, etc.)
  - Go to this link and provide additional information. When asked, inform the following endpoint (change only domain name if necessary, keep the rest of this URI): https://cacaoidb.duckdns.org/login/oauth2/code/azure
  - You may need to include additional callback endpoint if using for development environment: https://127.0.0.1:8888/login/oauth2/code/azure
- Take note of these information and keep secret (do not publish anywhere):
  - Application ID (client)
  - Active Directory ID (tenant)
  - Object ID
  - Secret (valid for one year or two year)
- Consent permission to application 
  - Go to panel 'Permissions of API'
  - Click que button/check mark 'Consent'
- Include OpenId claims (email, family_name and given_name) at Token Configuration   
- You must inform these information in the configuration file 'app_config', at the deployment environment, accessible only by root user
  - azure.activedirectory.tenant-id=<copy here you tenant ID from Microsoft>
  - spring.security.oauth2.client.registration.azure.client-id=<copy here your client ID from Microsoft>
  - spring.security.oauth2.client.registration.azure.client-secret=<copy here you secret from Microsoft>
  - azure.activedirectory.user-group.allowed-groups=Users
  - spring.cloud.azure.telemetry.enable=false
  - azure.mediaservices.allow-telemetry=false
  - azure.activedirectory.allow-telemetry=false
  
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

sudo usermod -a -G docker ec2-user

sudo systemctl enable docker

<logout and and login again>

sudo curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

sudo chmod +x /usr/local/bin/docker-compose

sudo ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose

echo 'vm.max_map_count=262144' >> /etc/sysctl.conf

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

### Create self signed certificates for every component in the stack

    docker cp conf/instance.yml es01:/usr/share/elasticsearch/config/instance.yml
    docker exec -it es01 /usr/share/elasticsearch/bin/elasticsearch-certutil cert --keep-ca-key --pem --in /usr/share/elasticsearch/config/instance.yml --out /usr/share/elasticsearch/CERT/certs.zip

Confirm with 'Y' if prompted to create 'CERT' directory.

### Copy certificate files to local

    docker cp es01:/usr/share/elasticsearch/CERT/certs.zip conf/certs.zip
    unzip conf/certs.zip -d conf
    
If 'unzip' is not recognized as a valid command, install it first (e.g.: yum install unzip)

### Copy certificate files to each node of ElasticSearch and Kibana

    chmod u+x conf/copy_certs.sh
    conf/copy_certs.sh

### Stop running components
    docker-compose stop web etl validator kibana es01 es02 es03

### Copy 'docker-compose.override.ssl.yml' to 'docker-compose.override.yml'
    cp docker-compose.override.ssl.yml docker-compose.override.yml 

WARNING

If you are using a different version of 'docker-compose.yml', you may need to check the
contents of 'docker-compose.override.ssl.yml' and see if they match you current
configuration (e.g. check the services names and the number of them)

### Start Elastic nodes
    docker-compose up -d es01 es02 es03

### Access shell inside ElasticSearch container
    docker exec -it es01 /bin/bash

### Generates random passwords for ElasticSearch components
    /usr/share/elasticsearch/bin/elasticsearch-setup-passwords auto -u "https://es01:9200"
    
When prompted, confirm with 'y' and ENTER

VERY IMPORTANT!!! The previous command will print all the passwords at the console. COPY AND PASTE these random passwords to a safe notebook to continue with the configuration. After all the configurations are done, you SHOULD delete these annotations.
    
Take note of login and passwords for all user accounts that were generated (most important ones: 'elastic' and 'kibana')

### Exit shell from es01

### Test arbitrary HTTP call to the ElasticSearch with user/password. Change the {password} below with the actual password that has been generated
    docker exec -it es01 curl -k https://es01:9200 -u elastic:{password}

It should respond with a JSON content with some information, including something like this: "tagline" : "You Know, for Search"

### Add to .env file at the host the password that was generated for 'kibana' user. Change the {password} below with the actual password that has been generated
    echo KIBANA_PASSWORD={password} >> .env
 
 ### Start Kibana
    docker-compose up -d kibana
 
### Check LOG entries for errors (<CTRL+C> to terminate LOG after a while)
    docker exec -it kibana tail -f /usr/share/kibana/logs/kibana.log
    
### Test arbitrary HTTP call to the Kibana with user/password. Change the {password} below with the actual password that has been generated
    docker exec -it kibana curl -k https://kibana:5601/kibana/api/spaces/space -u elastic:{password}
    
It should respond with a JSON content with some information about the Kibana default 'space', among others.
    
### Include all permissions to the Application (replace {elastic password here} with the password generated in previous step for user account 'elastic')
    echo 'es.user=elastic' | tee -a app_config_web app_config_etl app_config_validator
    
    echo 'es.password={elastic password here}' | tee -a app_config_web app_config_etl app_config_validator
    
    echo 'es.ssl=true' | tee -a app_config_web app_config_etl app_config_validator

    echo 'es.ssl.verifyhost=false' | tee -a app_config_web app_config_etl app_config_validator
    
### Start Application nodes
    docker-compose up -d web etl validator
    
### Check LOG entries for errors (<CTRL+C> to terminate LOG after a while)
    docker logs --follow web

### Start Proxy node
    docker-compose up -d proxy
    
### Test access to Kontaktu using your browser

___

## Troubleshooting

### "502 Bad Gateway" error at web browser

If this error appears whenever trying to access the server using a web browser, it may be a problem with the 'proxy' component or with the 'web' component'.

1) Check if the 'web' component is running

Use the following command to check if the 'web' component is running. 

    docker-compose ps web
    
If the service is running, it should output something like this:

    Name    Command     State   Ports
    ---------------------------------
    web    ./setup.sh   Up

If the 'web' component is not running, start it using this command line:

    docker-compose up -d web
    
2) Check if the 'proxy' can reach the 'web' component

Use the following command to check if the 'web' component is reachable from the proxy component

    docker exec -it proxy curl -I http://web:8080
    
The above command should output a couple of lines, starting with this one:

    HTTP/1.1 200
    
In case of error (e.g. 'Connection refused'), try to fix this by starting any missing components.

    docker-compose up -d

3) If all the components are running, try to reload the proxy service

For some reason the 'proxy' internal process may be stale. Try to reload the process with this command:

    docker exec -it proxy /usr/sbin/nginx -c /config/nginx/nginx.conf -s reload
    
