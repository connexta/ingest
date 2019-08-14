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

## Inspecting
The MIS service is deployed with (Springfox) **Swagger UI**. This library uses Spring Boot
annotations to create documentation for the service endpoints. To view Swagger UI in a local
deployment, enter this URL into a web browser:

`http://127.0.0.1:9041/swagger-ui.html`

The Ingest and MIS services are deployed with Spring Boot Actuator. To view the Actuator
endpoints in a local deployment, enter this URL into a web browser:

`http://127.0.0.1:9041/actuator/`
