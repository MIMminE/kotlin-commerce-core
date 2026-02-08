package nuts.commerce.paymentservice.usecase

import nuts.commerce.paymentservice.model.event.IncomingEvent
import nuts.commerce.paymentservice.port.payment.PaymentProvider
import nuts.commerce.paymentservice.port.repository.InboxRepository
import nuts.commerce.paymentservice.port.repository.PaymentRepository
import java.time.Instant
import java.util.UUID
    private val paymentRepository: PaymentRepository,
    private val paymentProvider: PaymentProvider
    private val paymentInboxRepository: InboxRepository,
) {

    data class Command(
        val orderId: UUID,
    fun execute(request : IncomingEvent.PaymentRequested){
