package nuts.project.commerce.domain.common

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createAt: Instant
        protected set

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: Instant
        protected set
}