package nuts.commerce.paymentservice.model.exception

import nuts.commerce.paymentservice.model.domain.PaymentStatus
import java.util.UUID

sealed class PaymentException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    class InvalidTransition(
        val paymentId: UUID?,
        val from: PaymentStatus,
        val to: PaymentStatus
    ) : PaymentException("invalid transition: $from -> $to (paymentId=$paymentId)")

    class InvalidCommand(message: String) : PaymentException(message)

    class PaymentFailed(paymentId: UUID?, val reason: String?) :
        PaymentException($$"payment failed: paymentId=$$paymentId, reason=$reason")
}