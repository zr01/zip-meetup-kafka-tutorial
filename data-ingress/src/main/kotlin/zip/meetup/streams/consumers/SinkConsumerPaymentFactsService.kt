package zip.meetup.streams.consumers

import mu.KotlinLogging
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.kstream.KStream
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import zip.meetup.payment.PaymentCompletedEvent
import zip.meetup.payment.PaymentInitiatedEvent
import zip.meetup.payment.PaymentNotificationSentEvent
import zip.meetup.repositories.FctPaymentEntity
import zip.meetup.repositories.FctPaymentsRepository
import zip.meetup.services.DataService
import java.time.Instant
import java.util.function.Consumer

private val log = KotlinLogging.logger { }
private val acceptedEventsForStaging = listOf(
    PaymentInitiatedEvent.`SCHEMA$`.name,
    PaymentCompletedEvent.`SCHEMA$`.name,
    PaymentNotificationSentEvent.`SCHEMA$`.name,
)

/**
 * This service is hooked into consuming data from a topic called `payment-events`.
 * The configuration is found in this project's application.yaml.
 * The focus of the conversation is how you can just use kafka as part of your data pipeline needs.
 *
 * However, we should be mindful that the specific use case for this code is for an application that
 * continuously produces events to kafka.
 *
 * The use case for this is to straight out create a Fact from the Data Stream. This creates a pipeline
 * where the Data Stream IS the `Staging`.
 */
@Service
class SinkConsumerPaymentFactsService(
    private val service: DataService,
    private val repository: FctPaymentsRepository
) {

    @Bean
    fun sinkConsumerPaymentFacts() = Consumer<KStream<String, SpecificRecord>> { input ->
        input
            /**
             * To avoid possible issues with events that are not supported, we opt to filter them out here.
             */
            .filter { _, value ->
                acceptedEventsForStaging.contains(value.schema.name)
            }
            /**
             * Each event (or message) is then processed to be converted to a Fact.
             * Note that we are not going to be referring to a Dimension in this discussion.
             */
            .foreach { paymentId, payment ->
                processPaymentFact(paymentId, payment)
            }
    }

    private fun processPaymentFact(paymentId: String, event: SpecificRecord) {
        log.debug { "Processing Payment Fact $paymentId" }

        /**
         * We try to see if the record exists in the Data Warehouse, this is similar to trying to pick it up via a SELECT statement.
         */
        repository.findById(paymentId)
            /**
             * The first block of code we see here is what we can execute if the record exists in the DW
             */
            .ifPresentOrElse({ dwRecord ->
                /**
                 * Depending on the event, we then merge all the data from the event into the DW record
                 */
                when (event.schema.name) {
                    PaymentCompletedEvent.`SCHEMA$`.name ->
                        dwRecord.merge(event as PaymentCompletedEvent)

                    PaymentNotificationSentEvent.`SCHEMA$`.name ->
                        dwRecord.merge(event as PaymentNotificationSentEvent)
                }
                /**
                 * We then update the record in the DW for this Fact
                 */
                service.storeFact(dwRecord)
                log.info { "Updated payment $paymentId from ${event.schema.name}" }
            }, {
                /**
                 * This second block of code we see here is what we execute to CREATE the initial record
                 */
                service.storeFact(
                    (event as PaymentInitiatedEvent).initRecord(paymentId),
                )
                log.info { "Inserted payment $paymentId from ${event.schema.name}" }
            })

        /**
         * This is one of the simplest forms of managing a Fact from multiple events using Kafka.
         * As you've seen, it did not have to create a huge batch of statements.
         * In the Data Streaming world, we only ever deal with 1 record at a time.
         * This allows us to build/rebuild as accurately as possible based on the events.
         */
        log.info { "Processed payment Fact $paymentId" }
    }
}

private fun PaymentInitiatedEvent.initRecord(paymentId: String) = FctPaymentEntity(
    paymentId = paymentId,
    accountId = accountId.toString(),
    merchantId = merchantId.toString(),
    amount = amount,
    paymentStartedAt = Instant.ofEpochMilli(timestamp),
)

private fun FctPaymentEntity.merge(event: PaymentCompletedEvent) = apply {
    isPaymentSuccess = event.isSuccess
    paymentCompletedAt = Instant.ofEpochMilli(event.timestamp)
    paymentProcessingTime = event.timestamp - paymentStartedAt.toEpochMilli()
}

private fun FctPaymentEntity.merge(event: PaymentNotificationSentEvent) = apply {
    isNotificationSent = event.isSent
    notificationSentAt = Instant.ofEpochMilli(event.timestamp)
}