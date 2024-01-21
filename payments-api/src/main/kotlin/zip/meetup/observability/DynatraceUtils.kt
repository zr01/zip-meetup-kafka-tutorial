package zip.meetup.observability

import com.dynatrace.oneagent.sdk.OneAgentSDKFactory
import com.dynatrace.oneagent.sdk.api.OneAgentSDK
import com.dynatrace.oneagent.sdk.api.enums.ChannelType
import com.dynatrace.oneagent.sdk.api.enums.MessageDestinationType
import com.dynatrace.oneagent.sdk.api.enums.SDKState
import com.dynatrace.oneagent.sdk.api.infos.MessagingSystemInfo
import mu.KotlinLogging
import org.apache.avro.specific.SpecificRecord
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import reactor.core.publisher.Sinks
import java.lang.Exception
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger { }
private val isDynatraceEnabled = false
val agent = OneAgentSDKFactory.createInstance()

private fun OneAgentSDK.safety(): OneAgentSDK? = when (currentState) {
    SDKState.ACTIVE -> this
    else -> null
}

fun OneAgentSDK.setMessagingInfo(
    messagingServiceName: String,
    topicName: String,
    destinationType: MessageDestinationType,
    channelType: ChannelType,
    brokerHostName: String? = null
): MessagingSystemInfo? = safety()?.createMessagingSystemInfo(messagingServiceName, topicName, destinationType, channelType, brokerHostName)

fun MessagingSystemInfo.setIncomingTrace(dynatraceTraceTag: String, process: () -> Unit) {
    val tracer = if (isDynatraceEnabled) agent.safety()?.traceIncomingMessageProcess(this) else null
    if (tracer != null) {
        tracer.setDynatraceStringTag(dynatraceTraceTag)
        log.info { "Trace activated -> $dynatraceTraceTag" }
        tracer.start()
        try {
            process()
        } catch (e: Exception) {
            tracer.error(e)
            throw e
        } finally {
            tracer.end()
        }
    } else {
        process()
    }
}

fun Sinks.Many<Message<SpecificRecord>>.publish(key: String, event: SpecificRecord, messagingSystemInfo: MessagingSystemInfo?) {
    val msgBuilder = MessageBuilder
        .withPayload(event)
        .setHeader(KafkaHeaders.KEY, key)

    val tracer = if (isDynatraceEnabled) messagingSystemInfo?.let { info -> agent.safety()?.traceOutgoingMessage(info) } else null
    if (tracer != null) {
        tracer.start()
        msgBuilder.setHeader(OneAgentSDK.DYNATRACE_MESSAGE_PROPERTYNAME, tracer.dynatraceStringTag)
    }

    var isSuccessfullySent = false
    while (!isSuccessfullySent) {
        val result = tryEmitNext(msgBuilder.build())
        if (result.isSuccess) {
            isSuccessfullySent = true
        } else {
            TimeUnit.MILLISECONDS.sleep(1)
        }
    }
    tracer?.end()
}