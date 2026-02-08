package nuts.commerce.orderservice.port.repository

import nuts.commerce.orderservice.model.OrderSaga
import java.util.UUID

interface OrderSagaRepository {
    fun save(saga: OrderSaga): OrderSaga
    fun findByOrderId(orderId: UUID): OrderSaga?
}
