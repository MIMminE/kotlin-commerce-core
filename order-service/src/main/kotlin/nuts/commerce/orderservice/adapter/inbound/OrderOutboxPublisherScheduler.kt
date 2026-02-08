package nuts.commerce.orderservice.adapter.inbound

import nuts.commerce.orderservice.usecase.PublishOrderOutboxUseCase
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "order.outbox.publish",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class OrderOutboxPublisherScheduler(
    private val publishOrderOutboxUseCase: PublishOrderOutboxUseCase
) {

    @Scheduled(fixedDelayString = "\${order.outbox.publish.fixed-delay-ms}")
    fun publishOutbox() {
        publishOrderOutboxUseCase.publishPendingOutboxMessages()
    }
}