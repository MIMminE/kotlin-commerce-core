package nuts.commerce.orderservice.application.port.repository

import nuts.commerce.orderservice.model.domain.OrderSaga
import java.util.UUID

interface OrderSagaRepository {
    fun save(saga: OrderSaga): OrderSaga
    fun findByOrderId(orderId: UUID): OrderSaga?
}
