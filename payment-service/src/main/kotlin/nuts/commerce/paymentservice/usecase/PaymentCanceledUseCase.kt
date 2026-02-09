package nuts.commerce.paymentservice.usecase

import nuts.commerce.paymentservice.port.repository.OutboxRepository
import nuts.commerce.paymentservice.port.repository.PaymentRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PaymentCanceledUseCase(
    private val paymentRepository: PaymentRepository,
    private val outboxRepository: OutboxRepository
) {

    fun execute(command: PaymentCancelCommand) {

    }
}

data class PaymentCancelCommand(val paymentId: UUID)