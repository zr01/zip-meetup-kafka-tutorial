package zip.meetup.streams.consumers

import mu.KotlinLogging
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.kstream.KStream
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import zip.meetup.payment.PaymentCompletedEvent
import zip.meetup.payment.PaymentInitiatedEvent
import zip.meetup.payment.PaymentNotificationSentEvent
import zip.meetup.repositories.StgEventsEntity
import zip.meetup.repositories.UnstructuredEventEntity
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
class SinkConsumerStagePaymentsService(
    private val service: DataService
) {

    @Bean
    fun sinkConsumerStagePayments() = Consumer<KStream<String, SpecificRecord>> { incoming ->
        incoming
            .filter { _, value ->
                acceptedEventsForStaging.contains(value.schema.name)
            }
            .foreach { paymentId, event ->
                processToStaging(paymentId, event)
            }
    }

    private fun processToStaging(paymentId: String, event: SpecificRecord) {
        log.debug { "Received ${event.schema.name} for $paymentId" }

        when (event.schema.name) {
            PaymentInitiatedEvent.`SCHEMA$`.name -> (event as PaymentInitiatedEvent).toEventEntity(paymentId)
            PaymentCompletedEvent.`SCHEMA$`.name -> (event as PaymentCompletedEvent).toEventEntity(paymentId)
            PaymentNotificationSentEvent.`SCHEMA$`.name -> (event as PaymentNotificationSentEvent).toEventEntity(paymentId)
            else -> null
        }?.also { entity ->
            service.stagePayment(entity)
        } ?: log.warn { "Event ${event.schema.name} for $paymentId is not supported" }

        log.info { "Processed ${event.schema.name} for $paymentId" }
    }
}

private fun PaymentInitiatedEvent.toEventEntity(paymentId: String) = StgEventsEntity(
    eventName = PaymentInitiatedEvent.`SCHEMA$`.name,
    eventRef = paymentId,
    unstructuredEvent = UnstructuredEventEntity(
        mapOf(
            "paymentId" to paymentId,
            "accountId" to accountId.toString(),
            "merchantId" to merchantId.toString(),
            "amount" to amount,
        ),
    ),
    eventTime = Instant.ofEpochMilli(timestamp),
    eventSource = source.toString(),
)

private fun PaymentCompletedEvent.toEventEntity(paymentId: String) = StgEventsEntity(
    eventName = PaymentCompletedEvent.`SCHEMA$`.name,
    eventRef = paymentId,
    unstructuredEvent = UnstructuredEventEntity(
        mapOf(
            "paymentId" to paymentId,
            "accountId" to accountId.toString(),
            "merchantId" to merchantId.toString(),
            "amount" to amount,
            "isSuccess" to isSuccess,
        ),
    ),
    eventTime = Instant.ofEpochMilli(timestamp),
    eventSource = source.toString(),
)

private fun PaymentNotificationSentEvent.toEventEntity(paymentId: String) = StgEventsEntity(
    eventName = PaymentNotificationSentEvent.`SCHEMA$`.name,
    eventRef = paymentId,
    unstructuredEvent = UnstructuredEventEntity(
        mapOf(
            "paymentId" to paymentId,
            "accountId" to accountId.toString(),
            "isNotificationSent" to isSent,
        ),
    ),
    eventTime = Instant.ofEpochMilli(timestamp),
    eventSource = source.toString(),
)