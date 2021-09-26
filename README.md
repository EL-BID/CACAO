# CACAO

## What is CACAO

Sistema para Consulta y Almacenamiento de dados Contables y Apoyo Organizativo

___

## Minimalist setup for DEVELOPMENT 

For running the web component locally at a development desktop with minimal setup, you should follow these steps:

1. Install, configure and start a node of ElasticSearch (version 7.14.1) locally. At least one node should be enough.

2. Install, configure and start a kafka server locally. Download the docker-compose.yml from https://raw.githubusercontent.com/confluentinc/cp-all-in-one/6.2.0-post/cp-all-in-one/docker-compose.yml and run docker-compose up -d

3. Compile/build the ***CACAO Web project*** . If you are using an IDE such as Eclipse, the automatic build should be enough. 

4. Run the application using developer application properties, referring to the property file using command line arguments. You may use the internal file 'dev.properties' like this:

> --spring.config.additional-location=classpath:dev.properties

5. Access the front page using your browser. Confirm/bypass the security warning related to the unsafe self-signed certificate being used with the developer configuration.

> https://127.0.0.1:8888/

6. For a initial empty database, use the following login credentials:

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
