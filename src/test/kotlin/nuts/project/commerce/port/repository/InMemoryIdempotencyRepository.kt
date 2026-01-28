package nuts.project.commerce.port.repository

import nuts.project.commerce.application.exception.UniqueConstraintViolationException
import nuts.project.commerce.application.port.repository.IdempotencyRepository
import nuts.project.commerce.domain.common.Idempotency
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryIdempotencyRepository : IdempotencyRepository {

    private data class UKey(val scopeId: UUID, val action: Idempotency.ActionType, val idemKey: UUID)

    private val byId = ConcurrentHashMap<UUID, Idempotency>()
    private val byUniqueKey = ConcurrentHashMap<UKey, UUID>()


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

        val ukey = UKey(scopeId, action, idemKey)

        val existingId = byUniqueKey.putIfAbsent(ukey, idempotency.id) // 키가 없을때만 원자적으로 값을 넣어라를 수행한다.
        if (existingId != null) { // concurrentHashMap은 null 값을 허용하지 않기에 null 이라는 것은 존재하지 않는다는 의미
            throw UniqueConstraintViolationException(
                "Idempotency with scopeId=$scopeId, action=$action, idemKey=$idemKey already exists"
            )
        }

        byId[idempotency.id] = idempotency
        return idempotency
    }

    override fun find(id: UUID): Idempotency? = byId[id]

    override fun findByScopeIdAndActionAndIdemKey(
        scopeId: UUID,
        action: Idempotency.ActionType,
        idemKey: UUID
    ): Idempotency {
        val ukey = UKey(scopeId, action, idemKey)
        val id = byUniqueKey[ukey]
            ?: throw NoSuchElementException("Idempotency not found: scopeId=$scopeId, action=$action, idemKey=$idemKey")

        return byId[id]
            ?: throw IllegalStateException("Unique index points to missing entity. id=$id, scopeId=$scopeId, action=$action, idemKey=$idemKey")
    }
}