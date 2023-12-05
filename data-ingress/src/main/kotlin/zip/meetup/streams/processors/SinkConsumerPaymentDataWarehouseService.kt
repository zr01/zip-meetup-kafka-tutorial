package zip.meetup.streams.processors

import mu.KotlinLogging
import org.apache.kafka.streams.kstream.KStream
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import zip.meetup.payment.PaymentRecord
import zip.meetup.repositories.StgPaymentEntity
import zip.meetup.services.PaymentsDataService
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.function.Consumer

private val log = KotlinLogging.logger { }

@Service
class SinkConsumerPaymentDataWarehouseService(
    private val service: PaymentsDataService
) {

    @Bean
    fun sinkConsumerStagePayments() = Consumer<KStream<String, PaymentRecord>> { incoming ->
        incoming.foreach { paymentId, event ->
            processToStaging(paymentId, event)
        }
    }

    private fun processToStaging(paymentId: String, event: PaymentRecord) {
        log.debug { "Staging data for payment:$paymentId account:${event.accountId}" }
        service.stagePayment(event.toStgPaymentEntity())
        log.info { "Staged data for payment:$paymentId account:${event.accountId}" }
    }
}

private fun PaymentRecord.toStgPaymentEntity() = StgPaymentEntity(
    paymentId = paymentId.toString(),
    accountId = accountId.toString(),
    amount = amount,
    merchantId = merchantId.toString(),
    isPaymentSuccess = isPaymentSuccess,
    isNotificationSent = isNotificationSent,
    eventTime = OffsetDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.of("UTC")),
    eventSource = source.toString(),
)