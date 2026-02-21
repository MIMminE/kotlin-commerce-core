package nuts.commerce.inventoryservice.adapter.system

import nuts.commerce.inventoryservice.usecase.OutboxPublishUseCase
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@ConditionalOnProperty(
    prefix = "system.outbox-publisher",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class OutboxPublisher(private val outboxUseCase: OutboxPublishUseCase) {

    @Scheduled(fixedDelayString = $$"${system.outbox-publisher.fixed-delay:5000}")
    fun execute() {
        outboxUseCase.execute()
    }
}