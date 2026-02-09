package nuts.commerce.paymentservice.usecase

import nuts.commerce.paymentservice.port.repository.OutboxRepository
import nuts.commerce.paymentservice.port.repository.PaymentRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PaymentConfirmedUseCase(
    private val paymentRepository: PaymentRepository,
    private val outboxRepository: OutboxRepository
) {

    fun execute(command: PaymentConfirmCommand) {

    }
}

data class PaymentConfirmCommand(val paymentId: UUID)