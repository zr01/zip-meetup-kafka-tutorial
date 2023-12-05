package zip.meetup.repositories

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Table(name = "stg_payments")
@Entity
data class StgPaymentEntity(
    @get:Id
    @GeneratedValue
    @Column(name = "event_id")
    var eventId: Long = 0L,

    @Column(name = "payment_id")
    var paymentId: String = "",

    @Column(name = "account_id")
    var accountId: String = "",

    @Column(name = "amount")
    var amount: Int = 0,

    @Column(name = "merchant_id")
    var merchantId: String = "",

    @Column(name = "is_payment_success")
    var isPaymentSuccess: Boolean? = null,

    @Column(name = "is_notification_sent")
    var isNotificationSent: Boolean? = null,

    @Column(name = "event_time")
    var eventTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "event_source")
    var eventSource: String = ""
)

@Repository
interface StgPaymentRepository : CrudRepository<StgPaymentEntity, Long>