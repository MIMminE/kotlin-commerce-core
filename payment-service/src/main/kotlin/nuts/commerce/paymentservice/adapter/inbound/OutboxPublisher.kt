package nuts.commerce.paymentservice.adapter.inbound

import nuts.commerce.paymentservice.usecase.PublishOutboxUseCase
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@ConditionalOnProperty(
    prefix = "payment.outbox.publisher",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class OutboxPublisher(private val outboxUseCase: PublishOutboxUseCase) {

    @Scheduled(fixedDelayString = $$"${payment.outbox.publisher.fixed-delay:5000}")
    fun execute() {
        outboxUseCase.execute()
    }
}