package nuts.commerce.paymentservice.usecase

import nuts.commerce.paymentservice.port.repository.OutboxRepository
import org.springframework.stereotype.Component

@Component
class PublishOutboxUseCase(private val outboxRepository: OutboxRepository) {

}