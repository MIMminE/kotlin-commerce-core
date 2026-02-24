package nuts.commerce.paymentservice.adapter.system

import nuts.commerce.paymentservice.usecase.PublishOutboxUseCase
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
class OutboxPublisher(private val outboxUseCase: PublishOutboxUseCase) {

    @Scheduled(fixedDelayString = $$"${system.outbox-publisher.fixed-delay}")
    fun execute() {
        val claimOutboxResult = outboxUseCase.claim()
        if(claimOutboxResult.size > 0) {
            outboxUseCase.publish(claimOutboxResult)
        }
    }
}