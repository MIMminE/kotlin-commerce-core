package nuts.commerce.inventoryservice.adapter.inbound

import nuts.commerce.inventoryservice.usecase.OutboxPublishUseCase
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@ConditionalOnProperty(
    prefix = "inventory.outbox.publisher",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class OutboxPublisher(private val outboxUseCase: OutboxPublishUseCase) {

    @Scheduled(fixedDelayString = $$"${inventory.outbox.publisher.fixed-delay:5000}")
    fun execute() {
        outboxUseCase.execute()
    }
}