package zip.meetup.services

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import zip.meetup.repositories.FctPaymentEntity
import zip.meetup.repositories.FctPaymentsRepository
import zip.meetup.repositories.StgEventsEntity
import zip.meetup.repositories.StgEventsRepository

private val log = KotlinLogging.logger { }

interface DataService {
    fun stagePayment(payment: StgEventsEntity): StgEventsEntity
    fun storeFact(fact: FctPaymentEntity): FctPaymentEntity
}

@Service
class DataServiceImpl(
    private val stagingRepository: StgEventsRepository,
    private val factRepository: FctPaymentsRepository
) : DataService {

    @Transactional
    override fun stagePayment(payment: StgEventsEntity): StgEventsEntity {
        log.debug { "Inserting payment:${payment.eventRef} source:${payment.eventSource}" }
        return stagingRepository.save(payment)
            .also {
                log.info { "Inserted ${payment.eventRef} account:${payment.eventSource}" }
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