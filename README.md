# Zip Meetup's Kafka Stream Tutorial

This project uses `Kotlin`, `Spring Boot 3.1`, and `Kafka`.

## Project Structure

```text
zip-meetup-tutorial
+--- .api-test
+--- .docker
+--- kafka-avro
+--- payments-api
+--- rewards-api
```

### .api-test

This directory contains the test scripts for this project.

### .docker

This directory contains the containers we require to run the project. This is automatically started when the projects start.

If you wish to start this manually, run the following command in the root of this project:

```shell
docker-compose -f .docker/dev-compose.yaml up -d
```

Run the command to stop the containers:

```shell
docker-compose -f .docker/dev-compose.yaml down
```

### kafka-avro

This is a sub-module where we generate the AVRO schema classes for the project.

This is really important, this project does not automatically retain the source code that is generated from the local machine. It is meant to be
re-generated through CI/CD. In order to circumvent this, make sure to run the command below.

```shell
./gradlew :kafka-avro:run --args="compile schema schemas src/main/java 
```

The `args` is composed of `compile schema {directoryOfAvscFiles} {pathToSourceCodeOutput}`

### payments-api

This sub-module is the `existing` system in the Tutorial. It hosts the following endpoints:

```http request
### Make a Payment
POST http://localhost:8085/payment
Content-Type: application/json

{
  "accountId": "222",
  "amount": 200,
  "merchantId": "mid1"
}

### Query by account - payments
GET http://localhost:8085/payment?account_id=222
```

This sub-module is responsible for processing a `Payment` and also to retrieve payments based on `accountId`.

`PaymentInitiatedEvent` and `PaymentCompletedEvent` is published from this service after any events stemming from the processing of the `Payment`.

This sub-module is also responsible for consuming the latter events and projecting the state of the payment as a `PaymentRecord`.

### rewards-api

This sub-module is the focus of the tutorial. It hosts the following endpoints:

```http request
### Get rewards for account 222
GET http://localhost:8086/accounts/222/rewards
```

This sub-module is responsible for consuming the event `PaymentCompletedEvent` and calculate the rewards earned.

`RewardsEarnedRecord` is generated as `PaymentCompletedEvent` that are eligible for rewards are consumed.

## Following this Tutorial

1. To learn more about AVRO, go to `kafka-avro/README.md`
2. To learn more about publishing to `Kafka` as `events` occur, go to `payments-api/README.md`
3. To learn more about building the Topology from another source, go to `rewards-api/README.md`