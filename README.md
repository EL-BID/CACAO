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

sudo sysctl -w vm.max_map_count=262144

4. Clone GIT Repository

git clone https://github.com/gustavohbf/cacao.git

cd cacao

5. BUILD with Docker Compose

docker-compose build

6. RUN with Docker Compose

docker-compose up -d
