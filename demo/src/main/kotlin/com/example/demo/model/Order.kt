package com.example.demo.model

import jakarta.persistence.*
import lombok.Data
import java.util.UUID

@Entity
@Table(name = "orders")
@Data
class Order(
    @Id
    var id: UUID,

    var userId: Long,

    var status: OrderStatus,

    var stock: Long,

    @Version
    var version: Long? = null
) {
    companion object {
        fun create(
            id : UUID = UUID.randomUUID(),
            userId: Long,
            stock: Long = (1..100).random().toLong()
        ) = Order(
            id = id,
            userId = userId,
            status = OrderStatus.PENDING,
            stock = stock
        )
    }

    fun changeStatus(newStatus: OrderStatus) {
        this.status = newStatus
    }
}

enum class OrderStatus {
    PENDING,
    COMPLETED,
    CANCELLED
}