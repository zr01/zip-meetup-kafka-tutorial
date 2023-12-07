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

/**
 * This service is hooked into consuming data from a topic called `payment-events`.
 * The configuration is found in this project's application.yaml.
 * The focus of the conversation is how you can just use kafka as part of your data pipeline needs.
 *
 * However, we should be mindful that the specific use case for this code is for an application that
 * continuously produces events to kafka.
 *
 * The use case for this is to `Stage` the data in the Data Warehouse. Allowing further transformation from
 * unstructured events that we can utilize later in Data Warehouse pipelines without depending on Kafka.
 */
@Service
class SinkConsumerStagePaymentsService(
    private val service: DataService
) {

    @Bean
    fun sinkConsumerStagePayments() = Consumer<KStream<String, SpecificRecord>> { incoming ->
        incoming
            /**
             * To avoid possible issues with events that are not supported, we opt to filter them out here.
             */
            .filter { _, value ->
                acceptedEventsForStaging.contains(value.schema.name)
            }
            /**
             * Each event (or message) is then processed to be inserted into the `stg_events` table.
             */
            .foreach { paymentId, event ->
                processToStaging(paymentId, event)
            }
    }

    private fun processToStaging(paymentId: String, event: SpecificRecord) {
        log.debug { "Received ${event.schema.name} for $paymentId" }

        /**
         * Based on the incoming event name we then convert it to an Unstructured Event Data Format.
         * This allows your `Staging` to be able to transform data as it needs to based on the `event_name`.
         * By leveraging on an `event_ref` that can serve as the key for all the events, it allows any transformation
         * or information extraction that is only tied to a single entity.
         */
        when (event.schema.name) {
            PaymentInitiatedEvent.`SCHEMA$`.name -> (event as PaymentInitiatedEvent).toEventEntity(paymentId)
            PaymentCompletedEvent.`SCHEMA$`.name -> (event as PaymentCompletedEvent).toEventEntity(paymentId)
            PaymentNotificationSentEvent.`SCHEMA$`.name -> (event as PaymentNotificationSentEvent).toEventEntity(paymentId)
            else -> null
        }?.also { entity ->
            /**
             * This only ever inserts the data to the `Staging` table `stg_events`
             */
            service.stagePayment(entity)
        } ?: log.warn { "Event ${event.schema.name} for $paymentId is not supported" }

        /**
         * This use case is fairly simple, existing DW already allow for this mechanism to happen by connecting to existing
         * Data Sources and pulling data on the delta of the data that is contained in a table.
         *
         * `Staging` allows any DE and DA to do further analysis before creating data pipelines. Having the raw information
         * will serve as a good springboard to create data pipelines for analytics and monitoring.
         */
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