package nuts.commerce.paymentservice.event.inbound

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID

class PaymentInboundEvent(
    val eventId: UUID,
    val orderId: UUID,
    val eventType: InboundEventType,

    @field:JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "eventType"
    )
    @field:JsonSubTypes(
        JsonSubTypes.Type(value = PaymentCreatePayload::class, name = "PAYMENT_CREATE_REQUEST"),
        JsonSubTypes.Type(value = PaymentConfirmPayload::class, name = "PAYMENT_CONFIRM_REQUEST"),
        JsonSubTypes.Type(value = PaymentReleasePayload::class, name = "PAYMENT_RELEASE_REQUEST")
    )
    val payload: InboundPayload
)

sealed interface InboundPayload

data class PaymentCreatePayload(
    val amount: Long,
    val currency: String = "KRW",
) : InboundPayload

data class PaymentConfirmPayload(
    val paymentId: UUID
) : InboundPayload

data class PaymentReleasePayload(
    val paymentId: UUID
) : InboundPayload

enum class InboundEventType {
    PAYMENT_CREATE_REQUEST,
    PAYMENT_CONFIRM_REQUEST,
    PAYMENT_RELEASE_REQUEST
}