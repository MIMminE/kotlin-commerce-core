package nuts.commerce.paymentservice.usecase

import nuts.commerce.paymentservice.port.repository.PaymentRepository

class PaymentConfirmedUseCase(
    private val paymentRepository: PaymentRepository,
    private val paymentInboxRepository: InboxRepository,
) {
}PaymentRequested