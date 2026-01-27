package nuts.project.commerce.application.port.payment

import nuts.project.commerce.domain.common.Money
import nuts.project.commerce.domain.payment.PaymentStatus
import java.time.Instant

data class CreatePaymentSessionRequest(
    val merchantOrderId: String,
    val amount: Money
)

data class CreatePaymentSessionResponse(
    val paymentId: String,
    val redirectUrl: String
)

// ---- confirm ----

data class ConfirmPaymentRequest(
    val paymentId: String
)

data class ConfirmPaymentResponse(
    val paymentId: String,
    val status: PaymentStatus,
    val occurredAt: Instant
)

// ---- cancel ----

data class CancelPaymentRequest(
    val paymentId: String
)

data class CancelPaymentResponse(
    val paymentId: String,
    val status: PaymentStatus,
    val occurredAt: Instant
)

// ---- refund ----

data class RefundPaymentRequest(
    val paymentId: String,
    val amount: Money
)

data class RefundPaymentResponse(
    val paymentId: String,
    val status: PaymentStatus,
    val occurredAt: Instant
)

// ---- get ----

data class GetPaymentRequest(
    val paymentId: String
)

data class PaymentSnapshot(
    val paymentId: String,
    val merchantOrderId: String,
    val status: PaymentStatus,
    val amount: Money,             // 최초 결제 금액
    val refundedAmount: Money,     // 누적 환불 금액(0부터)
    val updatedAt: Instant
)