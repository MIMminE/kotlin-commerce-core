package nuts.project.commerce.application.port.repository

import nuts.project.commerce.application.exception.UniqueConstraintViolationException
import nuts.project.commerce.domain.common.Idempotency
import java.util.UUID

interface IdempotencyRepository {

    /**
     * @throws UniqueConstraintViolationException 이미 같은 (scopeId, action, idemKey)가 존재할 때
     */
    fun save(scopeId: UUID, action: Idempotency.ActionType, idemKey: UUID): Idempotency
    fun find(id: UUID): Idempotency?
    fun findByScopeIdAndActionAndIdemKey(scopeId: UUID, action: Idempotency.ActionType, idemKey: UUID): Idempotency
}
