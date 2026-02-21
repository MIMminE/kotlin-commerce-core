package nuts.commerce.productservice.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "product_event_inbox",
    uniqueConstraints = [UniqueConstraint(columnNames = ["idempotency_key"])]
)
class ProductEventInbox protected constructor(

    @Id
    val inboxId: UUID,

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    val idempotencyKey: UUID,

    @Column(name = "payload", nullable = false, updatable = false)
    val payload: String
) {

    companion object {
        fun create(
            idempotencyKey: UUID,
            payload: String
        ): ProductEventInbox {
            return ProductEventInbox(
                inboxId = UUID.randomUUID(),
                idempotencyKey = idempotencyKey,
                payload = payload
            )
        }
    }
}