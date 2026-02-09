package nuts.commerce.paymentservice.usecase

import nuts.commerce.paymentservice.port.repository.OutboxRepository
import nuts.commerce.paymentservice.port.repository.PaymentRepository
import org.springframework.stereotype.Component

@Component
class PaymentRequestedUseCase(
    private val paymentRepository: PaymentRepository,
    private val outboxRepository: OutboxRepository
) {

    fun execute(command: PaymentRequestCommand) {

    }
}

data class PaymentRequestCommand( // TODO
    val orderId: String,
)