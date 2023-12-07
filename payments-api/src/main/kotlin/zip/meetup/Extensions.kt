package zip.meetup

import org.apache.avro.util.Utf8
import java.time.Instant
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicInteger

val EMPTY = "".utf8()

fun timeNowEpoch(): Long = Instant.now().toEpochMilli()
fun String.utf8() = Utf8(this)

val atomicCounter = AtomicInteger(1)
fun generateId() = (OffsetDateTime.now().toEpochSecond() + atomicCounter.getAndIncrement()).toString()