package nuts.commerce.orderservice.application.port.message

import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryMessageProducer(
    private val wiredConsumer: MessageConsumer? = null,
    private val mapper: (MessageProducer.ProduceMessage) -> MessageConsumer.PaymentResultMessage = { pm ->
        MessageConsumer.PaymentResultMessage(
            eventId = pm.eventId,
            eventType = pm.eventType,
            orderId = pm.aggregateId,
            payload = pm.payload,
            occurredAt = null,
            correlationId = null
        )
    }
) : MessageProducer {

    private val _produced = CopyOnWriteArrayList<MessageProducer.ProduceMessage>()
    val produced: List<MessageProducer.ProduceMessage> get() = _produced

    override fun produce(produceMessage: MessageProducer.ProduceMessage) {
        _produced += produceMessage
        wiredConsumer?.onPaymentResult(mapper(produceMessage))
    }

    fun clear() = _produced.clear()

    fun lastOrNull(): MessageProducer.ProduceMessage? = _produced.lastOrNull()

    fun findByAggregateId(aggregateId: UUID): List<MessageProducer.ProduceMessage> =
        _produced.filter { it.aggregateId == aggregateId }
}