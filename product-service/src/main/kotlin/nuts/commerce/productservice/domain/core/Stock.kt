package nuts.commerce.productservice.domain.core

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class Stock(
    @Column(name = "stock_available", nullable = false)
    var available: Int = 0,
    @Column(name = "stock_reserved", nullable = false)
    var reserved: Int = 0
) {
    fun reserve(qty: Int) {
        require(qty > 0) { "qty must be > 0" }
        require(available >= qty) { "not enough stock" }
        available -= qty
        reserved += qty
    }

    fun release(qty: Int) {
        require(qty > 0) { "qty must be > 0" }
        require(reserved >= qty) { "reserved stock is not enough" }
        reserved -= qty
        available += qty
    }

    fun deductReserved(qty: Int) {
        require(qty > 0) { "qty must be > 0" }
        require(reserved >= qty) { "reserved stock is not enough" }
        reserved -= qty
        // available은 이미 reserve에서 빠졌으므로 여기서는 reserved만 줄임
    }

    fun increase(qty: Int) {
        require(qty > 0) { "qty must be > 0" }
        available += qty
    }
}