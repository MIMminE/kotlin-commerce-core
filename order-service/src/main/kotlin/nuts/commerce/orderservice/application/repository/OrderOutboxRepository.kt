package nuts.commerce.orderservice.application.repository

import nuts.commerce.orderservice.domain.core.OrderOutboxEvent
import java.time.Instant
import java.util.UUID

interface OrderOutboxRepository {
    fun save(event: OrderOutboxEvent): OrderOutboxEvent
    fun findUnpublished(limit: Int): List<OrderOutboxEvent>

    // 성공 처리: 실제로 상태가 바뀌었으면 true, 이미 처리된 거면 false
    fun tryMarkPublished(eventId: UUID, publishedAt: Instant = Instant.now()): Boolean

    // 실패 처리도 "아직 PUBLISHED가 아닌 경우만" 업데이트되게 하는 게 안전
    fun markFailed(eventId: UUID, error: String, failedAt: Instant = Instant.now()): Boolean
}