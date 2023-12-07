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

@Service
class SinkConsumerPaymentFactsService(
    private val service: DataService,
    private val repository: FctPaymentsRepository
) {

    @Bean
    fun sinkConsumerPaymentFacts() = Consumer<KStream<String, SpecificRecord>> { input ->
        input
            .filter { _, value ->
                acceptedEventsForStaging.contains(value.schema.name)
            }
            .foreach { paymentId, payment ->
                processPaymentFact(paymentId, payment)
            }
    }

    private fun processPaymentFact(paymentId: String, event: SpecificRecord) {
        log.debug { "Processing Payment Fact $paymentId" }

        repository.findById(paymentId)
            .ifPresentOrElse({ dwRecord ->
                when (event.schema.name) {
                    PaymentCompletedEvent.`SCHEMA$`.name ->
                        dwRecord.merge(event as PaymentCompletedEvent)

                    PaymentNotificationSentEvent.`SCHEMA$`.name ->
                        dwRecord.merge(event as PaymentNotificationSentEvent)
                }
                service.storeFact(dwRecord)
                log.info { "Updated payment $paymentId from ${event.schema.name}" }
            }, {
                service.storeFact(
                    (event as PaymentInitiatedEvent).initRecord(paymentId)
                )
                log.info { "Inserted payment $paymentId from ${event.schema.name}" }
            })
        log.info { "Processed payment Fact $paymentId" }
    }
}

private fun PaymentInitiatedEvent.initRecord(paymentId: String) = FctPaymentEntity(
    paymentId = paymentId,
    accountId = accountId.toString(),
    merchantId = merchantId.toString(),
    amount = amount,
    paymentStartedAt = Instant.ofEpochMilli(timestamp)
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
