package nuts.commerce.paymentservice.domain


@Embeddable
data class Money(
    @Column(name = "amount", nullable = false)
    var amount: Long = 0L,

    @Column(name = "currency", nullable = false, length = 8)
    var currency: String = "KRW"
)