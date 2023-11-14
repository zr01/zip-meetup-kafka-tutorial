package zip.meetup.controllers

import mu.KotlinLogging
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import zip.meetup.generateId
import zip.meetup.services.EventPublisherService

private val log = KotlinLogging.logger { }

/**
 * In production, you'd have proper layering of interfaces to serve your controller.
 * This example is to just showcase it straight from 1 page.
 */
@RestController
@RequestMapping("/payment")
class PaymentController(
    private val eventPublisherService: EventPublisherService
) {

    @PostMapping
    fun postPayment(
        @RequestBody request: PaymentRequest
    ): PaymentResponse {
        // 1. Initiate the payment by generating an ID
        val paymentId = generateId()
        val initEvent = request.toPaymentInitiatedEvent(request.accountId)
        eventPublisherService.publishPaymentEvent(paymentId, initEvent)

        // 2. Process a payment and emit the event
        // Fail: amount <= 0
        if (request.amount <= 0) {
            eventPublisherService.publishPaymentEvent(paymentId, initEvent.toPaymentCompletedEvent(request, false))
            throw PaymentFailedException(request)
        }

        // Success: amount > 0
        eventPublisherService.publishPaymentEvent(paymentId, initEvent.toPaymentCompletedEvent(request, true))

        return request.toResponseWithPaymentId(paymentId)
    }
}

private fun PaymentRequest.toResponseWithPaymentId(paymentId: String) = PaymentResponse(accountId, amount, paymentId, merchantId)

data class PaymentRequest(
    val accountId: String,
    val amount: Int,
    val merchantId: String
)

data class PaymentResponse(
    val accountId: String,
    val amount: Int,
    val paymentId: String,
    val merchantId: String
)

class PaymentFailedException(request: PaymentRequest) : RuntimeException("Payment failed for ${request.accountId} for amount ${request.amount}")