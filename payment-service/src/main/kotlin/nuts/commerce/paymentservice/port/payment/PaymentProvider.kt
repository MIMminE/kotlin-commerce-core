package nuts.commerce.paymentservice.port.payment

import java.util.UUID
import java.util.concurrent.CompletableFuture

interface PaymentProvider {
    val providerName: String
    fun createPayment(request: CreatePaymentRequest): CompletableFuture<CreatePaymentResult>
    fun commitPayment(providerPaymentId: UUID, eventId: UUID): CompletableFuture<PaymentStatusUpdateResult>
    fun releasePayment(providerPaymentId: UUID, eventId: UUID): CompletableFuture<PaymentStatusUpdateResult>
}

data class CreatePaymentRequest(
    val orderId: UUID,
    val paymentId: UUID,
    val amount: Long,
    val currency: String,
)

sealed interface CreatePaymentResult {
    data class Success(val providerPaymentId: UUID, val requestPaymentId: UUID) : CreatePaymentResult
    data class Failure(val reason: String, val requestPaymentId: UUID) : CreatePaymentResult
}

sealed interface PaymentStatusUpdateResult {
    data class Success(val providerPaymentId: UUID) : PaymentStatusUpdateResult
}