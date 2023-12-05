package zip.meetup.repositories

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Table(name = "fct_payments")
@Entity
data class FctPaymentEntity(
    @get:Id
    @Column(name = "payment_id")
    var paymentId: String = "",

    @Column(name = "account_id")
    var accountId: String = "",

    @Column(name = "merchant_id")
    var merchantId: String = "",

    @Column(name = "amount")
    var amount: Int = 0,

    @Column(name = "is_payment_success")
    var isPaymentSuccess: Boolean? = null,

    @Column(name = "is_notification_sent")
    var isNotificationSent: Boolean? = null,

    @Column(name = "payment_processing_time")
    var paymentProcessingTime: Long? = null,

    @Column(name = "payment_completed_at")
    var paymentCompletedAt: OffsetDateTime? = null
)

@Repository
interface FctPaymentsRepository : CrudRepository<FctPaymentEntity, String>