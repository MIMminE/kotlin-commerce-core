package nuts.commerce.orderservice.application.port.message

import nuts.commerce.orderservice.port.message.ReservationEventProducer
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryReservationEventProducer(
    private val wiredConsumer: MessageConsumer? = null,
    private val mapper: (ReservationEventProducer.ProduceEvent) -> MessageConsumer.PaymentResultMessage = { pm ->
        MessageConsumer.PaymentResultMessage(
            eventId = pm.eventId,
            eventType = pm.eventType,
            orderId = pm.aggregateId,
            payload = pm.payload,
            occurredAt = null,
            correlationId = null
        )
    },
    private val failWhen: (ReservationEventProducer.ProduceEvent) -> Throwable? = { null }
) : ReservationEventProducer {

    private val _produced = CopyOnWriteArrayList<ReservationEventProducer.ProduceEvent>()
    val produced: List<ReservationEventProducer.ProduceEvent> get() = _produced

    override fun produce(produceEvent: ReservationEventProducer.ProduceEvent) {
        failWhen(produceEvent)?.let { throw it }

        _produced += produceEvent
        wiredConsumer?.onPaymentResult(mapper(produceEvent))
    }

    fun clear() = _produced.clear()

    fun lastOrNull(): ReservationEventProducer.ProduceEvent? = _produced.lastOrNull()

    fun findByAggregateId(aggregateId: UUID): List<ReservationEventProducer.ProduceEvent> =
        _produced.filter { it.aggregateId == aggregateId }
}