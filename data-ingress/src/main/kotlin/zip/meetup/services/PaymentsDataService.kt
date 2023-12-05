package zip.meetup.services

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import zip.meetup.repositories.FctPaymentEntity
import zip.meetup.repositories.FctPaymentsRepository
import zip.meetup.repositories.StgPaymentEntity
import zip.meetup.repositories.StgPaymentRepository

private val log = KotlinLogging.logger { }

interface PaymentsDataService {
    fun stagePayment(payment: StgPaymentEntity): StgPaymentEntity
    fun storeFact(fact: FctPaymentEntity): FctPaymentEntity
}

@Service
class PaymentsDataServiceImpl(
    private val stagingRepository: StgPaymentRepository,
    private val factRepository: FctPaymentsRepository
) : PaymentsDataService {

    @Transactional
    override fun stagePayment(payment: StgPaymentEntity): StgPaymentEntity {
        log.debug { "Inserting payment:${payment.paymentId} account:${payment.accountId}" }
        return stagingRepository.save(payment)
            .also {
                log.info { "Inserted ${payment.paymentId} account:${payment.accountId}" }
            }
    }

    @Transactional
    override fun storeFact(fact: FctPaymentEntity): FctPaymentEntity {
        log.debug { "Upserting payment:${fact.paymentId} account:${fact.accountId}" }
        factRepository.findById(fact.paymentId)
            .ifPresentOrElse({ dbEntity ->
                dbEntity.apply {
                    isPaymentSuccess = fact.isPaymentSuccess
                    isNotificationSent = fact.isNotificationSent
                    paymentProcessingTime = fact.paymentProcessingTime
                    paymentCompletedAt = fact.paymentCompletedAt
                }
            }, {
                factRepository.save(fact)
            })
        return fact.also {
            log.info { "Upserted payment:${fact.paymentId} accout:${fact.accountId}" }
        }
    }
}