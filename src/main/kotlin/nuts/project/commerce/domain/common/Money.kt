package nuts.project.commerce.domain.common

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class Money(
    @Column(name = "amount", nullable = false)
    val amount: Long
) {
    init {
        require(amount >= 0) { "Amount must be non-negative" }
    }

    fun plus(other: Money) = Money(this.amount + other.amount)
    fun minus(other: Money): Money {
        require(this.amount >= other.amount) { "Resulting amount must be non-negative" }
        return Money(this.amount - other.amount)
    }
}