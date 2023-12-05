package zip.meetup

import org.apache.avro.util.Utf8
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicInteger

fun now() = timeNow().toString()
fun timeNow(): OffsetDateTime = OffsetDateTime.now()
fun String.utf8() = Utf8(this)
fun nowUtf8() = now().utf8()
fun CharSequence.utf8() = Utf8(this.toString().toByteArray())

val atomicCounter = AtomicInteger(1)
fun generateId() = (OffsetDateTime.now().toEpochSecond() + atomicCounter.getAndIncrement()).toString()