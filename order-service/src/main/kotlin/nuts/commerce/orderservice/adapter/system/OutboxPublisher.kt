package nuts.commerce.orderservice.adapter.system

import nuts.commerce.orderservice.usecase.OutboxPublishUseCase
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "system.outbox-publisher",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class OutboxPublisher(
    private val outboxPublishUseCase: OutboxPublishUseCase
) {

    @Scheduled(fixedDelayString = $$"${system.outbox-publisher.fixed-delay:5000}")
    fun publishOutbox() {
        println("[OutboxPublisher] Starting outbox publish process...")
        outboxPublishUseCase.execute()
    }
}