package nuts.commerce.inventoryservice.adapter.inbound

import jakarta.annotation.PostConstruct
import nuts.commerce.inventoryservice.usecase.OutboxPublishUseCase
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@ConditionalOnProperty(
    name = ["outbox.listener.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class OutboxPublisher(
    private val outboxUseCase: OutboxPublishUseCase
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        log.info("OutboxPublisher initialized")
    }

    @Scheduled(fixedDelayString = "\${inventory.outbox.fixed-delay:5000}")
    fun execute() {
        log.info("Starting OutboxPublisher execution")
        outboxUseCase.execute()
        log.info("Finished OutboxPublisher execution")
    }
}