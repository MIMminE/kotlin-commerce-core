package nuts.commerce.paymentservice.usecase

import nuts.commerce.paymentservice.port.repository.OutboxRepository
import nuts.commerce.paymentservice.port.repository.PaymentRepository

class PaymentCanceledUseCase(
    private val paymentRepository: PaymentRepository,
    private val paymentInboxRepository: InboxRepository,
) {
}