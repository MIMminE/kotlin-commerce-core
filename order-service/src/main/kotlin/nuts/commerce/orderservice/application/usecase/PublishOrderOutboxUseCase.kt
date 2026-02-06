package nuts.commerce.orderservice.application.usecase

import nuts.commerce.orderservice.application.port.message.MessageProducer
import nuts.commerce.orderservice.application.port.repository.OrderOutboxRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.*

@Service
class PublishOrderOutboxUseCase(
    private val orderOutboxRepository: OrderOutboxRepository,
    private val messageProducer: MessageProducer,
    private val transactionTemplate: TransactionTemplate,
    @Value("\${order.outbox.batch-size}") private val batchSize: Int,
    @Value("\${order.outbox.max-retries}") private val maxRetries: Int // 현재 미사용(추후 재시도 로직에 사용 예정)
) {

    fun publishPendingOutboxMessages() {
        val now = Instant.now()
        val ids = transactional { orderOutboxRepository.claimReadyToPublishIds(limit = batchSize) }

        if (ids.isEmpty()) return

        val publishedIds = mutableListOf<UUID>()

        orderOutboxRepository.findByIds(ids).forEach { outboxMessage ->
            runCatching {
                messageProducer.produce(
                    MessageProducer.ProduceMessage(
                        eventId = outboxMessage.outboxId,
                        eventType = outboxMessage.eventType.name,
                        payload = outboxMessage.payload,
                        aggregateId = outboxMessage.aggregateId
                    )
                )
            }.onSuccess {
                publishedIds += outboxMessage.outboxId
            }.onFailure { ex ->
                val cause = ex.cause ?: ex
                val error = cause.message ?: (cause::class.qualifiedName ?: "unknown")

                transactional {
                    val managed = orderOutboxRepository.findById(outboxMessage.outboxId) ?: return@transactional
                    managed.markFailed(
                        error = error,
                        now = now,
                    )
                }
            }
        }
        if (publishedIds.isEmpty()) return
        transactional {
            val eventList = orderOutboxRepository.findByIds(publishedIds)
            eventList.forEach { it.markPublished(now) }
        }
    }

    private fun <T> transactional(block: () -> T): T =
        transactionTemplate.execute { block() }!!
}