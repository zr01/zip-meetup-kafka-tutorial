package zip.meetup.services

import mu.KotlinLogging
import org.apache.avro.specific.SpecificRecord
import org.springframework.context.annotation.Bean
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Service
import reactor.core.publisher.Sinks
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

private val log = KotlinLogging.logger { }

interface EventPublisherService {
    fun publishPaymentEvent(accountId: String, event: SpecificRecord)
}

@Service
class EventPublisherServiceImpl : EventPublisherService {

    private val sinkPaymentEvents = Sinks.many().unicast().onBackpressureBuffer<Message<SpecificRecord>>()

    @Bean
    fun sourceProcessorPaymentEvents() = Supplier {
        sinkPaymentEvents.asFlux()
    }

    override fun publishPaymentEvent(accountId: String, event: SpecificRecord) {
        log.debug { "Publishing for key: $accountId for event ${event.schema.name}" }
        sinkPaymentEvents.publish(
            event.asMessageWithKey(accountId),
        )
        log.info { "Published for key: $accountId for event ${event.schema.name}" }
    }
}

private fun <T : Any> SpecificRecord.asMessageWithKey(key: T): Message<SpecificRecord> = MessageBuilder
    .withPayload(this)
    .setHeader(KafkaHeaders.KEY, key)
    .build()

private fun <T : Message<SpecificRecord>> Sinks.Many<T>.publish(event: T) {
    var isSuccessfullySent = false
    while (!isSuccessfullySent) {
        val result = tryEmitNext(event)
        if (result.isSuccess) {
            isSuccessfullySent = true
        } else {
            TimeUnit.MILLISECONDS.sleep(1)
        }
    }
}