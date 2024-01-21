package zip.meetup.streams

import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import zip.meetup.payment.PaymentRecord

class DynatraceKafkaProcessSupplier : Processor<String, SpecificRecord, String, PaymentRecord> {

    private var context: ProcessorContext<String, PaymentRecord>? = null

    override fun init(context: ProcessorContext<String, PaymentRecord>?) {
        this.context = context
        super.init(context)
    }

    override fun process(record: Record<String, SpecificRecord>?) {
        // Get header


        TODO("Not yet implemented")
    }
}