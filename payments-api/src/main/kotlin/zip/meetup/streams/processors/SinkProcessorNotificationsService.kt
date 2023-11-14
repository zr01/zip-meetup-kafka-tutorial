package zip.meetup.streams.processors

import mu.KotlinLogging
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.KStream
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import zip.meetup.payment.PaymentCompletedEvent
import zip.meetup.payment.PaymentNotificationSentEvent
import java.util.function.Function

private val log = KotlinLogging.logger { }

@Service
class SinkProcessorNotificationsService {

    @Bean
    fun sinkProcessorNotifications() = Function<KStream<String, SpecificRecord>, KStream<String, SpecificRecord?>> { stream ->
        stream
            .filter { _, event -> event.schema == PaymentCompletedEvent.`SCHEMA$` }
            .map { paymentId, completedEvent ->
                log.info { "Payment ID         : $paymentId" }
                log.info { "Notification Event : $completedEvent " }
                if (completedEvent is PaymentCompletedEvent) {
                    log.info { "Sending message to ${completedEvent.merchantId} for isSuccess: ${completedEvent.isSuccess} payment $paymentId" }
                    KeyValue(paymentId, completedEvent.toNotificationSent(true))
                } else {
                    null
                }
            }
    }
}

private fun PaymentCompletedEvent.toNotificationSent(isSent: Boolean) = PaymentNotificationSentEvent.newBuilder()
    .setAccountId(accountId)
    .setIsSent(isSent)
    .build()