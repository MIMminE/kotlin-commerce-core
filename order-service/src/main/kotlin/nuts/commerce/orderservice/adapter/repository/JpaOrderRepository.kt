package nuts.commerce.orderservice.adapter.repository

import nuts.commerce.orderservice.port.repository.OrderRepository
import nuts.commerce.orderservice.model.Order
import nuts.commerce.orderservice.model.OrderStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class JpaOrderRepository(private val orderJpa: OrderJpa) : OrderRepository {
    override fun save(order: Order): Order {
        return orderJpa.saveAndFlush(order)
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
        return orderJpa.findAllByUserId(userId, pageable)
    }

    override fun findByUserIdAndIdempotencyKey(
        userId: String,
        idempotencyKey: UUID
    ): Order? {
        try{
            val findByUserIdAndIdempotencyKey = orderJpa.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
            return  findByUserIdAndIdempotencyKey
        } catch (e: Exception) {
            throw IllegalStateException("Failed to find order by userId: $userId and idempotencyKey: $idempotencyKey", e)
        }
    }


    override fun updateStatus(
        orderId: UUID,
        expectStatus: OrderStatus,
        newStatus: OrderStatus
    ) {
        val n = orderJpa.updateStatus(orderId, expectStatus, newStatus)
        if (n == 0) {
            throw IllegalStateException("Order not updated for orderId: $orderId, expect: $expectStatus, new: $newStatus")
        }
    }
}

interface OrderJpa : JpaRepository<Order, UUID> {
    fun findByUserIdAndIdempotencyKey(userId: String, idempotencyKey: UUID): Order?
    fun findAllByUserId(userId: String, pageable: Pageable): Page<Order>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE Order o
           SET o.status = :newStatus
         WHERE o.orderId = :orderId
           AND o.status = :expectStatus
        """
    )
    fun updateStatus(orderId: UUID, expectStatus: OrderStatus, newStatus: OrderStatus): Int
}