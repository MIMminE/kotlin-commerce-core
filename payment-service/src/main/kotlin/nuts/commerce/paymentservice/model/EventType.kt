package nuts.commerce.paymentservice.model

enum class EventType {
    PAYMENT_CREATION,
    PAYMENT_CREATION_FAILED,
    PAYMENT_CONFIRMED,
    PAYMENT_RELEASED
}