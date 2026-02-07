package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ConfirmInventoryReservationUseCase(
    private val inventoryRepository: InventoryRepository
) {
    fun execute(inventoryId: UUID): Result {
        val inv = inventoryRepository.findById(inventoryId)
        return Result(inv.inventoryId, inv.quantity)
    }

    data class Result(val inventoryId: UUID, val quantity: Long)
}

