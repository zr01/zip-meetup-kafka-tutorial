package zip.meetup.controllers

import mu.KotlinLogging
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import zip.meetup.payment.PaymentRecord
import kotlin.streams.asStream

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/payment")
class PaymentQueryController(
    private val queryService: InteractiveQueryService
) {

    @GetMapping
    fun getPaymentsByAccountId(
        @RequestParam(name = "account_id", required = true) accountId: String
    ): List<Payment> {
        log.debug { "Querying payments by account ID $accountId" }
        val store: ReadOnlyKeyValueStore<String, PaymentRecord> = queryService
            .getQueryableStore(
                "payment-record-state-store",
                QueryableStoreTypes.keyValueStore(),
            )

        return store.all()
            .asSequence()
            .asStream()
            .filter { kv ->
                kv.value.accountId.toString() == accountId
            }.map { it.value.toPayment(it.key.toString()) }
            .toList()
            .sortedBy {
                it.paymentId
            }
            .also {
                log.debug { "Queried payments by account ID $accountId, size ${it.size}" }
            }
    }
}

data class Payment(
    val paymentId: String,
    val accountId: String,
    val amount: Int,
    val merchantId: String,
    val isSuccess: Boolean?,
    val isNotificationSent: Boolean?
)

private fun PaymentRecord.toPayment(paymentId: String) = Payment(
    paymentId,
    accountId.toString(),
    amount,
    merchantId.toString(),
    isPaymentSuccess,
    isNotificationSent,
)