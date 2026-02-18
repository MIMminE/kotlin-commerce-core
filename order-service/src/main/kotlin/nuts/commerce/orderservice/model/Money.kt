package nuts.commerce.orderservice.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class Money(
    @Column(name = "amount", nullable = false)
    var amount: Long = 0L,

    @Column(name = "currency", nullable = false, length = 8)
    var currency: String = "KRW"

) {
    companion object {
        fun zero(currency: String = "KRW") = Money(0L, currency)
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) {
            "Currency mismatch: $currency != ${other.currency}"
        }
        return Money(amount + other.amount, currency)
    }

    operator fun times(qty: Long): Money {
        require(qty >= 0) { "qty must be >= 0" }
        return Money(amount * qty, currency)
    }
}