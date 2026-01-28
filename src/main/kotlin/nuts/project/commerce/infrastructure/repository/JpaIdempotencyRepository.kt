package nuts.project.commerce.infrastructure.repository

import nuts.project.commerce.application.exception.UniqueConstraintViolationException
import nuts.project.commerce.application.port.repository.IdempotencyRepository
import nuts.project.commerce.domain.common.Idempotency
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaIdempotencyRepository(private val idempotencyJpa: IdempotencyJpa) : IdempotencyRepository {

    override fun save(
        scopeId: UUID,
        action: Idempotency.ActionType,
        idemKey: UUID
    ): Idempotency {
        val idempotency = Idempotency(
            scopeId = scopeId,
            action = action,
            idemKey = idemKey
        )
        try {
            return idempotencyJpa.save(idempotency)
        } catch (e: DataIntegrityViolationException) {
            if (e.cause is ConstraintViolationException) {
                throw UniqueConstraintViolationException("Idempotency with scopeId=$scopeId, action=$action, idemKey=$idemKey already exists")
            }
            throw e
        }
    }

    override fun find(id: UUID): Idempotency? {
        TODO("Not yet implemented")
    }

    override fun findByScopeIdAndActionAndIdemKey(
        scopeId: UUID,
        action: Idempotency.ActionType,
        idemKey: UUID
    ): Idempotency {
        TODO("Not yet implemented")
    }


    interface IdempotencyJpa : JpaRepository<Idempotency, UUID>
}