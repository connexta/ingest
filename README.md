# Ion-Ingest
[![Dependabot Status](https://api.dependabot.com/badges/status?host=github&repo=connexta/ion-ingest)](https://dependabot.com)
[![Known Vulnerabilities](https://snyk.io/test/github/connexta/ion-ingest/badge.svg)](https://snyk.io/test/github/connexta/ion-ingest)
[![CircleCI](https://circleci.com/gh/connexta/ion-ingest/tree/master.svg?style=svg)](https://circleci.com/gh/connexta/ion-ingest/tree/master)

## Prerequisites
* Java 11
* Docker daemon

## Working with IntelliJ or Eclipse
This repository uses [Lombok](https://projectlombok.org/), which requires additional configurations and plugins to work in IntelliJ / Eclipse.
Follow the instructions [here](https://www.baeldung.com/lombok-ide) to set up your IDE.

## Building
To just compile and build the projects:
```bash
./gradlew assemble
```
To do a full build with tests and the formatter:
```bash
./gradlew build
```

### Build Checks
#### OWASP
```bash
./gradlew dependencyCheckAnalyze --info
```
The report for each project can be found at build/reports/dependency-check-report.html.

#### Style
The build can fail because the static analysis tool, Spotless, detects an issue. To correct most Spotless issues:
```bash
./gradlew spotlessApply
```

For more information about spotless checks see
[here](https://github.com/diffplug/spotless/tree/master/plugin-gradle#custom-rules).

#### Tests
* Tests are run automatically with `./gradlew build`.
* To skip all tests, add `-x test`.
* Even if the tests fail, the artifacts are built and can be run.
* To change logging to better suit parallel builds pass `-Pparallel` or the `--info` flag
* To run a single test suite:
    ```bash
    ./gradlew module:test --test fullClassName
    ```

##### Integration Tests
* The integration tests require a Docker daemon.
* To skip integration tests, add `-PskipITests`.

## Configuration
Services can be configured with an external configuration file that will be applied to the docker container during
deployment. The configuration YAML files can be found under: `<PROJECT_ROOT>/configs/` and are not verison-controlled.
The properties in these files will be merged with any properties that you have configured in the service. The properties
in the external config file take precedence over config files that are built with the service.

## Running
### Run Locally
Start the service using one of the following:
* [`docker-compose`](#start-the-service-locally-via-docker-compose)
* [`docker stack`](#start-the-service-locally-via-docker-stack)

To determine the ports assigned to the services:
```bash
docker-compose ps
```

#### Start the Service Locally via `docker-compose`
1. A Docker network named `cdr` is needed to run via docker-compose.

    1. Determine if the network already exists:
        ```bash
        docker network ls
        ```
        If the network exists, the output includes a reference to it:
        ```bash
        NETWORK ID          NAME                DRIVER              SCOPE
        zk0kg1knhd6g        cdr                 overlay             swarm
        ```
    2. If the network has not been created:
        ```bash
        docker network create --driver=overlay --attachable cdr
        ```
2. Start the Docker service:
    ```bash
    docker-compose up -d
    ```

##### Helpful `docker-compose` Commands
* To stop the Docker service:
    ```bash
    docker-compose down
    ```
* To stream the logs to the console:
    ```bash
    docker-compose logs
    ```
* To stream the logs to the console for a specific service:
    ```bash
    docker-compose logs -f <service_name>
    ```

#### Start the Service Locally via `docker stack`
```bash
docker stack deploy -c docker-compose.yml cdr
```

##### Helpful `docker stack` Commands
* To stop the Docker service:
    ```bash
    docker stack rm cdr
    ```
* To check the status of all services in the stack:
    ```bash
    docker stack services cdr
    ```
* To stream the logs to the console:
    ```bash
    docker service logs
    ```
* To stream the logs to the console for a specific service:
    ```bash
    docker service logs -f <service_name>
    ```

## Inspecting
The Ingest service is deployed with (Springfox) **Swagger UI**. This library uses Spring Boot
annotations to create documentation for the service endpoints. To view Swagger UI in a local
deployment, enter this URL into a web browser:

`http://127.0.0.1:9040/swagger-ui.html`

The Ingest is deployed with Spring Boot Actuator. To view the Actuator
endpoints in a local deployment, enter this URL into a web browser:

`http://127.0.0.1:9040/actuator/`
