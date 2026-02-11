package nuts.commerce.paymentservice.port.payment

import java.util.UUID
import java.util.concurrent.CompletableFuture

interface PaymentProvider {
    val providerName: String
    fun charge(request: ChargeRequest): CompletableFuture<ChargeResult>
    fun commitPayment(providerPaymentId: UUID, eventId: UUID): CompletableFuture<PaymentStatusUpdateResult>
    fun releasePayment(providerPaymentId: UUID, eventId: UUID): CompletableFuture<PaymentStatusUpdateResult>
}

data class ChargeRequest(
    val orderId: UUID,
    val paymentId: UUID,
    val amount: Long,
    val currency: String,
)

sealed interface ChargeResult {
    data class Success(val providerPaymentId: UUID, val requestPaymentId: UUID) : ChargeResult
    data class Failure(val reason: String, val requestPaymentId: UUID) : ChargeResult
}

sealed interface PaymentStatusUpdateResult {
    data class Success(val providerPaymentId: UUID) : PaymentStatusUpdateResult
    data class Failure(val reason: String, val providerPaymentId: UUID) : PaymentStatusUpdateResult
}