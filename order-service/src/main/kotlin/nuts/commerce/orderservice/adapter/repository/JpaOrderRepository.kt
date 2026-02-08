package nuts.commerce.orderservice.adapter.repository

import nuts.commerce.orderservice.port.repository.OrderRepository
import nuts.commerce.orderservice.model.Order
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class JpaOrderRepository(private val orderJpa: OrderJpa) : OrderRepository {
    override fun save(order: Order): Order {
        return orderJpa.save(order)
    }

    override fun findById(id: UUID): Order? {
        return orderJpa.findById(id).orElse(null)
    }

    override fun existsById(id: UUID): Boolean {
        return orderJpa.existsById(id)
    }

    override fun findAllByUserId(
        userId: String,
        pageable: Pageable
    ): Page<Order> {
        TODO("Not yet implemented")
    }

    override fun findByUserIdAndIdempotencyKey(
        userId: String,
        idempotencyKey: UUID
    ): Order? =
        orderJpa.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
}

interface OrderJpa : JpaRepository<Order, UUID> {
    fun findByUserIdAndIdempotencyKey(userId: String, idempotencyKey: UUID): Order?
}