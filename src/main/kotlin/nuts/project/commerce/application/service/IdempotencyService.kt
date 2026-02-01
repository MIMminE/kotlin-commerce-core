package nuts.project.commerce.application.service

import jakarta.transaction.Transactional
import nuts.project.commerce.application.exception.UniqueConstraintViolationException
import nuts.project.commerce.application.port.repository.IdempotencyRepository
import nuts.project.commerce.domain.common.Idempotency
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class IdempotencyService(private val idempotencyRepository: IdempotencyRepository) {

    @Transactional
    fun tryStart(scopeId: UUID, action: Idempotency.ActionType, idemKey: UUID): StartResult {
        try {
            val idempotency = idempotencyRepository.save(scopeId, action, idemKey)
            return StartResult.Started(
                id = idempotency.id,
                createdAt = idempotency.createdAt
            )
        } catch (e: UniqueConstraintViolationException) {
            val existing = idempotencyRepository.findByScopeIdAndActionAndIdemKey(scopeId, action, idemKey)
                ?: throw IllegalStateException("UniqueConstraintViolationException 발생했으나 기존 데이터가 존재하지 않습니다. scopeId=$scopeId, action=$action, idemKey=$idemKey")


            return StartResult.Existing(
                id = existing.id,
                status = existing.status,
                createdAt = existing.createdAt,
                updatedAt = existing.updatedAt
            )
        }
    }


    @Transactional
    fun markSucceeded(
        idempotencyId: UUID,
        resourceType: Idempotency.ResourceType,
        resourceId: UUID,
    ) {
        idempotencyRepository.find(idempotencyId)?.let { idempotency ->
            idempotency.status = Idempotency.Status.SUCCEEDED
            idempotency.resourceId = resourceId
            idempotency.resourceType = resourceType

        } ?: throw NoSuchElementException("Idempotency with id $idempotencyId not found")

    }

    @Transactional
    fun markFailed(idempotencyId: UUID, at: Instant = Instant.now()) {
        idempotencyRepository.find(idempotencyId)?.let { idempotency ->
            idempotency.status = Idempotency.Status.FAILED
        } ?: throw NoSuchElementException("Idempotency with id $idempotencyId not found")
    }

    fun get(scopeId: UUID, action: Idempotency.ActionType, idemKey: UUID): Idempotency =
        idempotencyRepository.findByScopeIdAndActionAndIdemKey(scopeId, action, idemKey)
}


sealed class StartResult {
    data class Started(
        val id: UUID,
        val createdAt: Instant
    ) : StartResult()

    data class Existing(
        val id: UUID,
        val status: Idempotency.Status,
        val resourceId: UUID? = null,
        val createdAt: Instant,
        val updatedAt: Instant
    ) : StartResult()
}
