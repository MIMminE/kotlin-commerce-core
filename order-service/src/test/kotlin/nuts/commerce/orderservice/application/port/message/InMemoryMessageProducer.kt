package nuts.commerce.orderservice.application.port.message

import nuts.commerce.orderservice.port.message.MessageProducer
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
    },
    private val failWhen: (MessageProducer.ProduceMessage) -> Throwable? = { null }
) : MessageProducer {

    private val _produced = CopyOnWriteArrayList<MessageProducer.ProduceMessage>()
    val produced: List<MessageProducer.ProduceMessage> get() = _produced

    override fun produce(produceMessage: MessageProducer.ProduceMessage) {
        failWhen(produceMessage)?.let { throw it }

        _produced += produceMessage
        wiredConsumer?.onPaymentResult(mapper(produceMessage))
    }

    fun clear() = _produced.clear()

    fun lastOrNull(): MessageProducer.ProduceMessage? = _produced.lastOrNull()

    fun findByAggregateId(aggregateId: UUID): List<MessageProducer.ProduceMessage> =
        _produced.filter { it.aggregateId == aggregateId }
}