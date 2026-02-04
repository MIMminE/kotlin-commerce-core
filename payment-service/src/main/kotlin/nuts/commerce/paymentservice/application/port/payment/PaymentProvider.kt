package nuts.commerce.paymentservice.application.port.payment

interface PaymentProvider {
    fun charge(request: ChargeRequest): ChargeResponse

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