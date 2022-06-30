# CACAO

## What is CACAO

This project was created for facilitating the use of accounting data by tax administrations. 

CACAO is the acronym in Spanish: "Sistema para Consulta y Almacenamiento de dados Contables y Apoyo Organizativo"

CACAO is a web platform developed according to the 12-Factor design pattern (for more information about '12-Factor' design, see https://12factor.net/). There are different parts of this application that run as autonomous internal services. 

Each part of CACAO runs inside a Docker container, so it’s possible to deploy the whole system in different platforms, including these options: cloud, on-premises, single-server, multiple servers.

It’s possible to scale up any one of them by running multiple instances according to different needs. For example, if the ‘validation phase’ becomes a bottleneck in a scenario with high workload, it’s possible to start multiple instances of the component related to ‘validation’. On the other hand, to increase concurrency of multiple users, it may be necessary to start multiple instances of the component related to the frontend (the ‘web’ component). So, the overall system is flexible enough to accommodate different workload needs.

___

## Minimum requirements for production environment

CACAO is designed to be used in a distributed environment with multiple servers for high performance. But it’s also possible to run CACAO with less hardware resources.

The simplest and easiest configuration of CACAO deployment consists of one single server with the following hardware characteristics:

* RAM Memory: 32 GB
* CPU: 4 cores x86_64
* Disk: 60 GB SSD
* OS: Linux 64 bits (RedHat, CentOS or similar)
* Connectivity to the Internet and accessible externally by ports 443 (HTTPS) and 80 (HTTP)
* Any public external domain name to be assigned to CACAO

___

## How to BUILD from source

Before running the installation procedure, please make sure you have the most recent versions for DOCKER and DOCKER-COMPOSE. Most problems with installation procedure are related to using obsolete version software. This has already been tested with Docker version 20.10.13 and Docker-Compose version 1.29.2 

For full-stack deployment, use DOCKER-COMPOSE for building all CACAO modules.

    docker-compose build
    
On the other hand, if the application has already been deployed before, use this command instead of above in order to build and update only the modified CACAO modules:

    docker-compose up --build -d
    
It's also possible to build specific modules using MAVEN under development environment, running the following command inside the corresponding module.

    mvn install

Please refer to additional documents available in this project for further information related to development environment and production deployment.

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
