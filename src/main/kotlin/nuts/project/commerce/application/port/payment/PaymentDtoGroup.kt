package nuts.project.commerce.application.port.payment

import nuts.project.commerce.domain.common.Money
import nuts.project.commerce.domain.payment.PaymentStatus
import java.time.Instant
import java.util.UUID

data class CreatePaymentSessionRequest(
    val merchantOrderId: UUID,
    val amount: Money
)

data class CreatePaymentSessionResponse(
    val paymentId: UUID,
    val pgProvider : String,
    val pgSessionId: String,
    val createAt: Instant
)

data class ConfirmPaymentRequest(
    val paymentId: UUID
)

data class ConfirmPaymentResponse(
    val paymentId: UUID,
    val status: PaymentStatus,
    val occurredAt: Instant
)

data class CancelPaymentRequest(
    val paymentId: UUID
)

data class CancelPaymentResponse(
    val paymentId: UUID,
    val status: PaymentStatus,
    val occurredAt: Instant
)

data class RefundPaymentRequest(
    val paymentId: UUID
)

data class RefundPaymentResponse(
    val paymentId: UUID,
    val status: PaymentStatus,
    val occurredAt: Instant
)

data class GetPaymentRequest(
    val paymentId: UUID
)

data class PaymentSnapshot(
    val paymentId: UUID,
    val merchantOrderId: UUID,
    val status: PaymentStatus,
    val amount: Money,
    val refundedAmount: Money,
    val updatedAt: Instant
)