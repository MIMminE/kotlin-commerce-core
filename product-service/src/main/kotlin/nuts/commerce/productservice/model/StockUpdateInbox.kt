package nuts.commerce.productservice.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "stock_update_inbox",
    uniqueConstraints = [UniqueConstraint(columnNames = ["reservation_id", "idempotency_key"])]
)
class StockUpdateInboxRecord protected constructor(

    @Id
    val inboxId: UUID,

    @Column(name = "order_id", nullable = false, updatable = false)
    val orderId: UUID,

    @Column(name = "reservation_id", nullable = false, updatable = false)
    val reservationId: UUID,

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    val idempotencyKey: UUID,

    @Column(name = "payload", nullable = false, updatable = false)
    val payload: String
) {

    companion object {
        fun create(
            orderId: UUID,
            reservationId: UUID,
            idempotencyKey: UUID,
            payload: String
        ): StockUpdateInboxRecord {
            return StockUpdateInboxRecord(
                inboxId = UUID.randomUUID(),
                orderId = orderId,
                reservationId = reservationId,
                idempotencyKey = idempotencyKey,
                payload = payload
            )
        }
    }
}