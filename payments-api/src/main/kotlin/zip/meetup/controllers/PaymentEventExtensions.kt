package zip.meetup.controllers

import zip.meetup.payment.PaymentCompletedEvent
import zip.meetup.payment.PaymentInitiatedEvent
import zip.meetup.timeNowEpoch
import zip.meetup.utf8

private val EVENT_SOURCE = "api".utf8()

fun PaymentRequest.toPaymentInitiatedEvent(accountId: String): PaymentInitiatedEvent = PaymentInitiatedEvent.newBuilder()
    .setAccountId(accountId.utf8())
    .setAmount(amount)
    .setMerchantId(merchantId.utf8())
    .setTimestamp(timeNowEpoch())
    .setSource(EVENT_SOURCE)
    .build()

fun PaymentInitiatedEvent.toPaymentCompletedEvent(request: PaymentRequest, isSuccess: Boolean): PaymentCompletedEvent =
    PaymentCompletedEvent.newBuilder()
        .setAccountId(accountId)
        .setAmount(request.amount)
        .setMerchantId(request.merchantId.utf8())
        .setIsSuccess(isSuccess)
        .setTimestamp(timeNowEpoch())
        .setSource(EVENT_SOURCE)
        .build()