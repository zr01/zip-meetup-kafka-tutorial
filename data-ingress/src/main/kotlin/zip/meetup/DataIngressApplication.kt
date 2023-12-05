package zip.meetup

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DataIngressApplication

fun main(args: Array<String>) {
    runApplication<DataIngressApplication>(*args)
}