package nuts.commerce.paymentservice.adapter.message

import nuts.commerce.paymentservice.model.event.OutgoingEvent
import nuts.commerce.paymentservice.port.message.PaymentEventProducer

class KafkaPaymentEventProducer : PaymentEventProducer {

    override fun produce(outgoingEvent: OutgoingEvent) {
        TODO("Not yet implemented")
    }

}