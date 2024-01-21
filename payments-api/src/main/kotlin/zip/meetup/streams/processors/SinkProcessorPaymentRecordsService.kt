package zip.meetup.streams.processors

import com.dynatrace.oneagent.sdk.api.OneAgentSDK
import com.dynatrace.oneagent.sdk.api.enums.ChannelType
import com.dynatrace.oneagent.sdk.api.enums.MessageDestinationType
import com.dynatrace.oneagent.sdk.api.infos.MessagingSystemInfo
import mu.KotlinLogging
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorSupplier
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import zip.meetup.EMPTY
import zip.meetup.observability.agent
import zip.meetup.observability.setIncomingTrace
import zip.meetup.observability.setMessagingInfo
import zip.meetup.payment.PaymentCompletedEvent
import zip.meetup.payment.PaymentInitiatedEvent
import zip.meetup.payment.PaymentNotificationSentEvent
import zip.meetup.payment.PaymentRecord
import zip.meetup.timeNowEpoch
import zip.meetup.utf8
import java.util.function.Function

private val log = KotlinLogging.logger { }
private val EVENT_SOURCE = "sink-processor-payment-records".utf8()

@Service
class SinkProcessorPaymentRecordsService {

    private val msgInfo: MessagingSystemInfo? = null

    private fun msgInfo(): MessagingSystemInfo? = msgInfo
        ?: agent.setMessagingInfo("kafka", "payment-events", MessageDestinationType.TOPIC, ChannelType.TCP_IP)

    @Bean
    fun sinkProcessorPaymentRecords() = Function<
        // Stream of events
        KStream<String, SpecificRecord>,
        // The upsert of the record to the KTable, and in most cases should be non-nullable!
        KTable<String, PaymentRecord>,
        > { stream ->

        stream
            .process(
                ProcessorSupplier<String, SpecificRecord, String, PaymentRecord> {
                    Processor { record ->
                        log.info { "ProcessorSupplier received ${record.key()} and ${record.value().schema.name}" }
                        val traceHeader = record?.headers()?.lastHeader(OneAgentSDK.DYNATRACE_MESSAGE_PROPERTYNAME)?.value()?.let { String(it) }
                        if (traceHeader != null) {
                            msgInfo()?.setIncomingTrace(traceHeader) {
                                log.info { "Set trace to message ${record.key()}:${record.value()}" }
                            }
                        }
                    }
                }
            )
            .groupByKey() // Aggregate incoming events into the PaymentRecord
            .aggregate(
                {
                    PaymentRecord.newBuilder()
                        .setAccountId(EMPTY)
                        .setPaymentId(EMPTY)
                        .setAmount(0)
                        .setMerchantId(EMPTY)
                        .setTimestamp(0L)
                        .setSource(EMPTY)
                        .build()
                },
                { paymentId, event, record ->
                    // Process each event as it comes and update the PaymentRecord

                    log.info { "Received payment with id of $paymentId" }
                    log.info { "Received event of ${event.schema.name}" }
                    log.info { "Before Updating record from KTable is $record" }
                    process(paymentId, event, record)
                },
                // Materialize as a state store to do record keeping
                Materialized.`as`("payment-record-state-store"),
            )
    }

    private fun process(paymentId: String, event: SpecificRecord, record: PaymentRecord): PaymentRecord {
        return when (event.schema.name) {
            PaymentInitiatedEvent.`SCHEMA$`.name -> {
                log.info { "Creating new PaymentRecord for $paymentId" }
                record.update(paymentId, event as PaymentInitiatedEvent)
            }

            PaymentCompletedEvent.`SCHEMA$`.name -> {
                log.info { "Updating PaymentRecord of $paymentId to completed" }
                record.update(event as PaymentCompletedEvent)
            }

            PaymentNotificationSentEvent.`SCHEMA$`.name -> {
                log.info { "Updating PaymentRecord of $paymentId to notification sent" }
                record.update(event as PaymentNotificationSentEvent)
            }

            else -> record
        }.also { updatedRecord ->
            log.info { "Update to KTable is $updatedRecord" }
        }
    }
}

private fun PaymentRecord.update(id: String, event: PaymentInitiatedEvent): PaymentRecord = apply {
    paymentId = id.utf8()
    accountId = event.accountId
    amount = event.amount
    merchantId = event.merchantId
    timestamp = timeNowEpoch()
    source = EVENT_SOURCE
}

private fun PaymentRecord.update(event: PaymentCompletedEvent): PaymentRecord = apply {
    isPaymentSuccess = event.isSuccess
    timestamp = timeNowEpoch()
    source = EVENT_SOURCE
}

private fun PaymentRecord.update(event: PaymentNotificationSentEvent): PaymentRecord = apply {
    isNotificationSent = event.isSent
    timestamp = timeNowEpoch()
    source = EVENT_SOURCE
}