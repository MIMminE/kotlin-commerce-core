package nuts.project.commerce.domain.common

import jakarta.persistence.*
import java.time.Instant


@Entity
@Table(
    name = "idempotency_records",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_idem",
            columnNames = ["scope_id", "action", "idem_key"]
        )
    ],
    indexes = [
        Index(name = "ix_idem_status_created", columnList = "status, created_at")
    ]
)
class Idempotency(

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var clientId: String = "",
    var operation: String = "",
    var idemKey: String = "",

    @Enumerated(EnumType.STRING)
    var status: IdemStatus = IdemStatus.IN_PROGRESS,

    var resourceId: String? = null,

    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
) {
    fun succeed(resourceId: String) {
        status = IdemStatus.SUCCEEDED
        this.resourceId = resourceId
        updatedAt = Instant.now()
    }

    fun fail() {
        status = IdemStatus.FAILED
        updatedAt = Instant.now()
    }

    companion object {
        fun inProgress(clientId: String, operation: String, idemKey: String): Idempotency =
            Idempotency(
                clientId = clientId,
                operation = operation,
                idemKey = idemKey,
                status = IdemStatus.IN_PROGRESS
            )
    }
}