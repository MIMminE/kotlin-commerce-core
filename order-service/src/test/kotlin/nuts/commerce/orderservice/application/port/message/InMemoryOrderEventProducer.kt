package nuts.commerce.orderservice.application.port.message

import nuts.commerce.orderservice.port.message.OrderEventProducer
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryOrderEventProducer(
    private val wiredConsumer: MessageConsumer? = null,
    private val mapper: (OrderEventProducer.ProduceEvent) -> MessageConsumer.PaymentResultMessage = { pm ->
        MessageConsumer.PaymentResultMessage(
            eventId = pm.eventId,
            eventType = pm.eventType,
            orderId = pm.aggregateId,
            payload = pm.payload,
            occurredAt = null,
            correlationId = null
        )
    },
    private val failWhen: (OrderEventProducer.ProduceEvent) -> Throwable? = { null }
) : OrderEventProducer {

    private val _produced = CopyOnWriteArrayList<OrderEventProducer.ProduceEvent>()
    val produced: List<OrderEventProducer.ProduceEvent> get() = _produced

    override fun produce(produceEvent: OrderEventProducer.ProduceEvent) {
        failWhen(produceEvent)?.let { throw it }

        _produced += produceEvent
        wiredConsumer?.onPaymentResult(mapper(produceEvent))
    }

    fun clear() = _produced.clear()

    fun lastOrNull(): OrderEventProducer.ProduceEvent? = _produced.lastOrNull()

    fun findByAggregateId(aggregateId: UUID): List<OrderEventProducer.ProduceEvent> =
        _produced.filter { it.aggregateId == aggregateId }
}