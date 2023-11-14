package zip.meetup

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PaymentsApiApplication

fun main(args: Array<String>) {
    runApplication<PaymentsApiApplication>(*args)
}