package zip.meetup.controllers

import mu.KotlinLogging
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import zip.meetup.reward.RewardsEarnedRecord

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/accounts")
class RewardsQueryController(
    // We setup the interactive query service here so we can see what is in the state store!
    private val queryService: InteractiveQueryService
) {

    // Standard GET endpoint to query how much is the accumulated rewards
    @GetMapping("/{accountId}/rewards")
    fun getRewardsByAccountId(
        @PathVariable accountId: String
    ): Rewards {
        log.debug { "Querying rewards by account ID $accountId" }

        // We hookup to the local state store in this instance of the service
        val store: ReadOnlyKeyValueStore<String, RewardsEarnedRecord> = queryService
            // This is the same name we had Materialized.`as`(...) earlier
            .getQueryableStore(
                "account-rewards-state-store",
                QueryableStoreTypes.keyValueStore(),
            )

        // Kafka only ever keeps 1 record in the state store
        // There's not even a query here, but just get the record from the store by accountId
        return store[accountId]?.toRewards(accountId) ?: throw NotFoundException("No Account Found")
    }
}

// This is our Response in the API Endpoint
data class Rewards(
    val accountId: String,
    val rewardsEarned: Int
)

// Kotlin syntactical sugar allows you to have explicit mapping of data!
private fun RewardsEarnedRecord.toRewards(accountId: String) = Rewards(accountId, rewardsBalance)