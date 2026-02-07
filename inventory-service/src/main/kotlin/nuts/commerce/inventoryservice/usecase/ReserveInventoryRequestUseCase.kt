package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import nuts.commerce.inventoryservice.model.OutboxEventType
import nuts.commerce.inventoryservice.model.OutboxRecord
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Component
class ReserveInventoryRequestUseCase(
    private val inventoryRepository: InventoryRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {
    fun execute(inventoryId: UUID, qty: Long): Result {
        val inv = inventoryRepository.findById(inventoryId)

        inv.decreaseQuantity(qty)

        val saved = inventoryRepository.save(inv)

        val payloadObj = InventoryUpdatedPayload(
            inventoryId = saved.inventoryId,
            productId = saved.productId,
            quantity = saved.quantity
        )
        val payload = objectMapper.writeValueAsString(payloadObj)
        val outbox =
            OutboxRecord.create(inventoryId = saved.inventoryId, eventType = OutboxEventType.INVENTORY_UPDATED, payload = payload)
        outboxRepository.save(outbox)

        return Result(saved.inventoryId, saved.quantity)
    }

    data class Result(val inventoryId: UUID, val quantity: Long)

    private data class InventoryUpdatedPayload(
        val inventoryId: UUID,
        val productId: UUID,
        val quantity: Long
    )
}
