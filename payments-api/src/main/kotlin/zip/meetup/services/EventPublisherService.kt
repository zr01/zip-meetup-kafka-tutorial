package zip.meetup.services

import com.dynatrace.oneagent.sdk.api.enums.ChannelType
import com.dynatrace.oneagent.sdk.api.enums.MessageDestinationType
import com.dynatrace.oneagent.sdk.api.infos.MessagingSystemInfo
import mu.KotlinLogging
import org.apache.avro.specific.SpecificRecord
import org.springframework.context.annotation.Bean
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Service
import reactor.core.publisher.Sinks
import zip.meetup.observability.agent
import zip.meetup.observability.publish
import zip.meetup.observability.setMessagingInfo
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

private val log = KotlinLogging.logger { }

interface EventPublisherService {
    fun publishPaymentEvent(accountId: String, event: SpecificRecord)
}

@Service
class EventPublisherServiceImpl : EventPublisherService {

    private val sinkPaymentEvents = Sinks.many().unicast().onBackpressureBuffer<Message<SpecificRecord>>()
    private var paymentEventInfo: MessagingSystemInfo? = null

    @Bean
    fun sourceProcessorPaymentEvents() = Supplier {
        sinkPaymentEvents.asFlux()
    }

    private fun paymentEventInfo(): MessagingSystemInfo? = paymentEventInfo
        ?: agent.setMessagingInfo("kafka", "payment-events", MessageDestinationType.TOPIC, ChannelType.TCP_IP)

    override fun publishPaymentEvent(accountId: String, event: SpecificRecord) {
        log.debug { "Publishing for key: $accountId for event ${event.schema.name}" }
        sinkPaymentEvents.publish(
            accountId,
            event,
            paymentEventInfo()
        )
        log.info { "Published for key: $accountId for event ${event.schema.name}" }
    }
}
