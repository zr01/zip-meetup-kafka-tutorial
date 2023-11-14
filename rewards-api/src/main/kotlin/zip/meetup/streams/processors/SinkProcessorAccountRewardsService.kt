package zip.meetup.streams.processors

import mu.KotlinLogging
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Materialized
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import zip.meetup.payment.PaymentCompletedEvent
import zip.meetup.reward.RewardsEarnedRecord
import java.util.function.Function

private val log = KotlinLogging.logger { }

@Service
class SinkProcessorAccountRewardsService {

    @Bean
    fun sinkProcessorEligiblePaymentRewards() = Function<
        // f(x) = y
        // This is the incoming stream from topic:payment-events
        KStream<String, SpecificRecord>,

        // We publish the eligible successful payments with >= 20 amount to topic:eligible-payment-rewards
        // We also re-map the event's key to the accountId!
        // If it does not meet the criteria, it will not publish anything to the topic
        KStream<String, PaymentCompletedEvent>
        > { stream ->
            // Treat this as a list
        stream
            // We only need the SUCCESSFUL Payments
            // Each Payment amount qualifies for 1 reward credit for every 20 spent
            .filter { _, event ->
                event is PaymentCompletedEvent && event.isSuccess && event.amount >= 20
            }.map { _, event ->
                // We know this is the event class as we filtered it earlier
                val paymentCompletedEvent = event as PaymentCompletedEvent
                // We take the accountId from the event
                val accountId = paymentCompletedEvent.accountId.toString()
                log.info { "Sending event $event with key $accountId to topic:eligible-payment-rewards" }

                // Mapped to new key, same event
                // Sent to topic:eligible-payment-rewards
                KeyValue(
                    accountId,
                    paymentCompletedEvent
                )
            }
    }

    @Bean
    fun sinkProcessorAccountRewards() = Function<
        // f(x) = y
        // This is the incoming stream from topic:eligible-payment-rewards
        KStream<String, PaymentCompletedEvent>,

        // We publish the latest state of the rewards balance after calculating it to topic:account-rewards
        // The upstream only ever sends eligible payments for a reward
        KTable<String, RewardsEarnedRecord>
        > { stream ->

        stream
            // Incoming events in the stream may be multiple records
            .groupByKey()

            // Aggregation is the act of folding multiple events into a single record
            .aggregate({
                // This is the initial record, think of this as the `new` object
                RewardsEarnedRecord.newBuilder().setRewardsBalance(0).build()
            }, { accountId, event, record ->
                // Calculated earned rewards
                val earnedRewardsFromEvent = event.amount / 20

                log.info { "Account $accountId current rewards is ${record.rewardsBalance}" }
                // Applying the rewardsBalance to the existing record
                record.rewardsBalance += earnedRewardsFromEvent
                log.info { "Account $accountId has spent ${event.amount}, earning reward $earnedRewardsFromEvent" }

                // This is the record we publish to topic:account-rewards containing the updated rewardsBalance
                record
            },
                // We set the state store name here
                // This is how we tell the service how to look for a queryable data, by giving the state store a name
                // Next, we see how you can query the calculated data via the RewardsQueryController
                Materialized.`as`("account-rewards-state-store")
            )
    }
}