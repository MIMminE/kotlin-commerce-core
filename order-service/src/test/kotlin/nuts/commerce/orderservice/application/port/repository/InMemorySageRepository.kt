package nuts.commerce.orderservice.application.port.repository

import nuts.commerce.orderservice.model.OrderSaga
import nuts.commerce.orderservice.port.repository.SageRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemorySageRepository : SageRepository {

    private val store: MutableMap<UUID, OrderSaga> = ConcurrentHashMap()

    override fun save(saga: OrderSaga): OrderSaga {
        store[saga.sageId] = saga
        return saga
    }

    override fun findByOrderId(orderId: UUID): OrderSaga? =
        store.values.firstOrNull { it.orderId == orderId }

    fun clear() = store.clear()
}
