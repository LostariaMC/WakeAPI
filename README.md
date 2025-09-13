# WakeAPI
REST API built with Spring Boot to manage the startup sessions of an OVH Public Cloud instance

## Requirements
This project requires the following to get started:
- Java version `17` or higher
- Maven version `3.9.11` or higher

## Setup
1. Go to `src/main/resources`
2. Copy `application.properties.example` and rename it to `application.properties`
3. Update the values in `application.properties` to match your environment

## Compilation
```bash
mvn clean package
```

## Running
```bash
java -jar target/WakeAPI-1.0-SNAPSHOT.jar
```
