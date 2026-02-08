package nuts.commerce.paymentservice.port.payment

interface PaymentProvider {
    fun charge(request: ChargeRequest): ChargeResponse
    fun commitPayment(providerPaymentId: String): Boolean
    fun releasePayment(providerPaymentId: String): Boolean

    data class ChargeRequest(
        val orderId: String,
        val amount: Long,
        val currency: String,
        val paymentMethod: String
    )

    data class ChargeResponse(
        val success: Boolean,
        val providerPaymentId: String?,
        val failureReason: String?
    )
}