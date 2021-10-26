# CACAO

## What is CACAO

Sistema para Consulta y Almacenamiento de dados Contables y Apoyo Organizativo

___

## Minimalist setup for DEVELOPMENT 

For running the web component locally at a development desktop with minimal setup, you should follow these steps:

1. Start a node of ElasticSearch (version 7.14.1) with docker-compose-dev.yml. Run "docker-compose -f docker-compose-dev.yml up -d".

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

6. Sigin at the user interface (see the 'Sign in' button at top right corner)

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
