# Kafka Avro

Avro is a format that enables governance over your data stream. Having strictly formatted data structures allow both the publisher and consumer teams
to coordinate changes and evolution of data as your needs grow.

The focus of this project is to simply provide a short, ready to use project that can allow you to use the `avsc` files.

## Project structure

```text
kafka-avro
+--- schemas
```

### schemas

This directory contains the raw `.avsc` files. This is how we generate the Avro objects to the `src/main/java` source package.

An `avsc` is the AVro SChema file, the format is typically like below:

```avro schema
[
  {
    "namespace": "zip.meetup.payment",
    "type": "record",
    "name": "PaymentInitiatedEvent",
    "fields": [
      {
        "name": "accountId",
        "type": "string"
      },
      {
        "name": "amount",
        "type": "int"
      },
      {
        "name": "merchantId",
        "type": "string"
      }
    ]
  }
]
```

Depending on how you want to group your schemas, make sure you typically just contain them in a single file to group logistically for certain events.

## CI/CD

This sub-module is meant to be built as part of the CI/CD of your project. The artifact (or `jar`) is to be pushed to your choice of package 
repository.

## Re-using the sub-module in this project

After creating a project in the root, you can import the generated classes by adding the line below in `build.gradle.kts`.

```kotlin
// This is the dependencies section of the file
dependencies {
    implementation(project(":kafka-avro"))
}
```