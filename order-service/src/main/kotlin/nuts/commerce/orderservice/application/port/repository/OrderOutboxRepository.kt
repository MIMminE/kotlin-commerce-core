package nuts.commerce.orderservice.application.port.repository

import nuts.commerce.orderservice.model.integration.OrderOutboxRecord
import java.time.Instant
import java.util.UUID

interface OrderOutboxRepository {
    fun save(event: OrderOutboxRecord): OrderOutboxRecord
    fun findById(id: UUID): OrderOutboxRecord?
    fun findByIds(ids: List<UUID>): List<OrderOutboxRecord>
    fun claimReadyToPublishIds(limit: Int): List<UUID>  // 퍼블리싱 대상에는 팬딩상태거나, 실패상태지만 아직 횟수가 남아있는 것들이 포함됨
    fun tryMarkPublished(eventId: UUID, publishedAt: Instant = Instant.now()): Boolean
    fun markFailed(eventId: UUID, error: String, failedAt: Instant = Instant.now()): Boolean
}