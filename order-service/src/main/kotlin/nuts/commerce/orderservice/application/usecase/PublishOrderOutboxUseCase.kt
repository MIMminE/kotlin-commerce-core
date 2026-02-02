package nuts.commerce.orderservice.application.usecase

import nuts.commerce.orderservice.application.port.message.MessagePublisher
import nuts.commerce.orderservice.application.port.repository.OrderOutboxRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.*

@Service
class PublishOrderOutboxUseCase(
    private val orderOutboxRepository: OrderOutboxRepository,
    private val messagePublisher: MessagePublisher,
    private val transactionTemplate: TransactionTemplate,
    @Value("\${order.outbox.batch-size}") private val batchSize: Int,
    @Value("\${order.outbox.max-retries}") private val maxRetries: Int
) {

    fun publishPendingOutboxMessages() {
        val now = Instant.now()
        val ids = transactional { orderOutboxRepository.claimReadyToPublishIds(limit = batchSize) }

        if (ids.isEmpty()) return

        val publishedIds = mutableListOf<UUID>()

        orderOutboxRepository.findByIds(ids).forEach { outboxMessage ->
            runCatching {
                messagePublisher.publish(
                    eventId = outboxMessage.id,
                    payload = outboxMessage.payload,
                    aggregateId = outboxMessage.aggregateId,
                    eventType = outboxMessage.eventType
                )
            }.onSuccess {
                publishedIds += outboxMessage.id
            }.onFailure { ex ->
                val cause = ex.cause ?: ex
                val error = cause.message ?: (cause::class.qualifiedName ?: "unknown")

                transactional {
                    val managed = orderOutboxRepository.findById(outboxMessage.id) ?: return@transactional
                    managed.markFailed(
                        error = error,
                        now = now,
                        maxRetries = maxRetries,
                        baseDelaySeconds = 1,
                        maxDelaySeconds = 60
                    )
                }
            }
        }
        if (publishedIds.isEmpty()) return
        transactional {
            val eventList = orderOutboxRepository.findByIds(publishedIds)
            eventList.forEach { it.markPublished() }
        }
    }

    private fun <T> transactional(block: () -> T): T =
        transactionTemplate.execute { block() }!!
}