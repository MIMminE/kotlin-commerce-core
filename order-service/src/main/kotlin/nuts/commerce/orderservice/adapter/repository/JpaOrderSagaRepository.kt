package nuts.commerce.orderservice.adapter.repository

import nuts.commerce.orderservice.port.repository.OrderSagaRepository
import nuts.commerce.orderservice.model.OrderSaga
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaOrderSagaRepository(private val sagaJpa: OrderSagaJpa) : OrderSagaRepository {
    override fun save(saga: OrderSaga): OrderSaga = sagaJpa.save(saga)

    override fun findByOrderId(orderId: UUID): OrderSaga? = sagaJpa.findByOrderId(orderId)
}

interface OrderSagaJpa : JpaRepository<OrderSaga, UUID> {
    fun findByOrderId(orderId: UUID): OrderSaga?
}
