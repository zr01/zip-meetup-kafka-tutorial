package zip.meetup

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RewardsApiApplication

fun main(args: Array<String>) {
    runApplication<RewardsApiApplication>(*args)
}