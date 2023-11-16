## Payments API

This service is an example of a service that produces events.

### Project structure

```text
payments-api
+--- src
     \--- main
          \--- kotlin
          \--- resources
```

## src/main/kotlin

The main application code is here. In order to run the service, run the command below:

```shell
./gradlew payments-api:bootRun
```

## How To Use

### Making a Payment

This can be done by doing the API call below. You can also refer to `/zip-meetup-tutorial/.api-test/payments.http` to see example API
calls.

#### API Request

```http request
### Make a Payment
POST http://localhost:8085/payment
Content-Type: application/json

{
  "accountId": "222",
  "amount": 200,
  "merchantId": "mid1"
}
```

#### API Response

```json
{
  "accountId": "222",
  "amount": 200,
  "paymentId": "1700000449",
  "merchantId": "mid1"
}
```

### Get Payments

Retrieving the payments by account ID. This is not stored in a database, it is stored in a local state store that is running in the same instance.

#### API Request

```http request
### Query by account - payments
GET http://localhost:8085/payment?account_id=222
```

#### API Response

```json
[
  {
    "paymentId": "1700000449",
    "accountId": "222",
    "amount": 200,
    "merchantId": "mid1",
    "isSuccess": true,
    "isNotificationSent": true
  }
]
```

## Application Topology

This application does not use a database to store its data, it uses the state store to project the events that has occurred to a single data model
that encapsulates the information from the events that have happened.

This is the Topology of the `payments-api` application. This has 2 different processors.

A `source processor` which generates the events for `PaymentInitiatedEvent` and `PaymentCompletedEvent` as a `Payment` occurs.

There are 2 `sink processors` which consumes events from the `source processor` and deals with events to handle another side effect. It also deals 
with creating the `state store` for each payment that happens.

To know more about Topologies check it out here in 
[Confluent's Stream Architecture Document](https://docs.confluent.io/platform/current/streams/architecture.html).

![Payments API Kafka Topology](./topology-kafka-payments.png)

### Source Processor

The source generates 2 messages for `POST /payment` call.

### Sink Processor -- Payment Record State Store

The `sink processor` that consumes from `payment-events` and produces the record to `payment-history` creates the state store for each payment record.

### Sink Processor -- Webhook Notification

The `sink processor` that consumes from `payment-events` and produces the record to `payment-events`, its purpose is to send a webhook notification 
after a `PaymentCompletedEvent` is consumed. It then produces `PaymentNotificationSentEvent` after sending it successfully.

### How it all works together

The image below shows you how an event affects each `Topic`, any message that is produced is always inserted.

Following the color code below, the `yellow` event shows you the effect on the topic as an `event` occurs. This initializes the record in the 
`state store`, and the `isSuccess` and `isNotificationSent` remains `null` as the `event` has not occurred yet.

The `red` event shows you the effect on `payment-history` as well, updating the `isSuccess` to `true`. Consequently, the `state store` will also have 
the value updated for `isSuccess` to `true` and `isNotificationSent` remains `null`.

The `green` event shows you the effect on `payment-history` similar to how `isSuccess` has updated both `Topics`.

The end result is that the `PaymentRecord` has consumed all the events and is the data model we have designed earlier.

![How the Data Stream Produces the Records](./topology-kafka-data-stream-how-it-works.png)
