package zip.meetup.repositories

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnTransformer
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.io.Serializable
import java.time.Instant

@Table(name = "stg_events")
@Entity
data class StgEventsEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stg_events_seq")
    @SequenceGenerator(name = "stg_events_seq", sequenceName = "stg_events_seq", allocationSize = 1)
    @Column(name = "event_id")
    var eventId: Long = 0L,

    @Column(name = "event_name")
    var eventName: String = "",

    @Column(name = "event_ref")
    var eventRef: String = "",

    @Column(name = "unstructured_event", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @ColumnTransformer(write = "?::jsonb")
    var unstructuredEvent: UnstructuredEventEntity = UnstructuredEventEntity(),

    @Column(name = "event_time")
    var eventTime: Instant = Instant.now(),

    @Column(name = "event_source")
    var eventSource: String = "",

    @Column(name = "event_received")
    var eventReceived: Instant = Instant.now()
)

data class UnstructuredEventEntity(
    val data: Map<String, Any> = emptyMap()
) : Serializable

@Repository
interface StgEventsRepository : CrudRepository<StgEventsEntity, Long>