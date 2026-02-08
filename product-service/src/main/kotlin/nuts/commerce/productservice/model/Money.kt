package nuts.commerce.productservice.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class Money(
    @Column(name = "amount", nullable = false)
    var amount: Long = 0L,

    @Column(name = "currency", nullable = false, length = 8)
    var currency: String = "KRW"
)