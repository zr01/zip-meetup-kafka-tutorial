package zip.meetup.repositories

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Table(name = "fct_payments")
@Entity
data class FctPaymentEntity(
    @Id
    @Column(name = "payment_id", insertable = true)
    var paymentId: String = "",

    @Column(name = "account_id")
    var accountId: String = "",

    @Column(name = "merchant_id")
    var merchantId: String = "",

    @Column(name = "amount")
    var amount: Int = 0,

    @Column(name = "payment_started_at")
    var paymentStartedAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),

    @Column(name = "payment_success")
    var isPaymentSuccess: Boolean? = null,

    @Column(name = "notification_sent")
    var isNotificationSent: Boolean? = null,

    @Column(name = "notification_sent_at")
    var notificationSentAt: Instant? = null,

    @Column(name = "payment_processing_time")
    var paymentProcessingTime: Long? = null,

    @Column(name = "payment_completed_at")
    var paymentCompletedAt: Instant? = null
)

@Repository
interface FctPaymentsRepository : CrudRepository<FctPaymentEntity, String>