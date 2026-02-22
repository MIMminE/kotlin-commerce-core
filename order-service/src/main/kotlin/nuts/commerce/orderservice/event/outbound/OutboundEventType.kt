package nuts.commerce.orderservice.event.outbound

enum class OutboundEventType(
    val eventClass: Class<*>
) {
    RESERVATION_CREATE_REQUEST(ReservationOutboundEvent::class.java),
    RESERVATION_CONFIRM_REQUEST(ReservationOutboundEvent::class.java),
    RESERVATION_RELEASE_REQUEST(ReservationOutboundEvent::class.java),
    PAYMENT_CREATE_REQUEST(PaymentOutboundEvent::class.java),
    PAYMENT_CREATE_FAILED(PaymentOutboundEvent::class.java),
    PAYMENT_CONFIRM_REQUEST(PaymentOutboundEvent::class.java),
    PAYMENT_RELEASE_REQUEST(PaymentOutboundEvent::class.java), ;
}