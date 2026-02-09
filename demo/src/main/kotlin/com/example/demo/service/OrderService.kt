package com.example.demo.service

import com.example.demo.model.Order
import com.example.demo.model.OrderStatus
import com.example.demo.repository.OrderRepository
import jakarta.transaction.Transactional
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository
) {

    @Transactional
    fun saveRandomOrder(userId: Long, id: UUID): Order {
        val order = Order.create(
            id = id,
            userId = userId
        )
        return orderRepository.save(order)
    }

    @Transactional
    fun changeStatus(orderId: UUID, status: OrderStatus) {
        val order = orderRepository.findById(orderId).orElseThrow {
            IllegalArgumentException("Order with id $orderId not found")
        }

        order.changeStatus(status)
        orderRepository.flush()
        orderRepository.findById(orderId).ifPresent {
            println("Order ID: ${it.id}, Status: ${it.status}, Version: ${it.version}")
        }
    }
}