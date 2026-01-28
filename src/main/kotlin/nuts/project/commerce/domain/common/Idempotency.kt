package nuts.project.commerce.domain.common

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID


@Entity
@Table(
    name = "idempotency",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_idem_scope_action_key",
            columnNames = ["scope_id", "action", "idem_key"]
        )
    ],
    indexes = [
        Index(name = "ix_idem_status_created", columnList = "status, created_at")
    ]
)
class Idempotency(

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),

    @Column(name = "scope_id", nullable = false, length = 64)
    var scopeId: UUID,

    @Column(name = "action", nullable = false, length = 64)
    @Enumerated(EnumType.STRING)
    var action: ActionType,

    @Column(name = "idem_key", nullable = false, length = 128)
    var idemKey: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: Status = Status.IN_PROGRESS,

    @Column(name = "resource_type", length = 32)
    var resourceType: ResourceType? = null,

    @Column(name = "resource_id", length = 64)
    var resourceId: UUID? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    enum class Status {
        IN_PROGRESS, SUCCEEDED, FAILED
    }

    enum class ResourceType {
        ORDER, PAYMENT, COUPON
    }

    enum class ActionType {
        PLACE_ORDER
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }
}