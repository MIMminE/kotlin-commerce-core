package nuts.commerce.orderservice.application.port.repository

import nuts.commerce.orderservice.model.domain.OrderSaga
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryOrderSagaRepository : OrderSagaRepository {

    private val store: MutableMap<UUID, OrderSaga> = ConcurrentHashMap()

    override fun save(saga: OrderSaga): OrderSaga {
        store[saga.id] = saga
        return saga
    }

    override fun findByOrderId(orderId: UUID): OrderSaga? =
        store.values.firstOrNull { it.orderId == orderId }

    fun clear() = store.clear()
}
