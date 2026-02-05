package nuts.commerce.inventoryservice.application.port.message

import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryInventoryCachePublisher : InventoryCachePublisher {

    data class Published(val inventoryId: UUID, val productId: UUID, val quantity: Long)

    private val _published = CopyOnWriteArrayList<Published>()
    val published: List<Published> get() = _published

    override fun publish(inventoryId: UUID, productId: UUID, quantity: Long) {
        _published += Published(inventoryId, productId, quantity)
    }

    fun clear() = _published.clear()
    fun lastOrNull(): Published? = _published.lastOrNull()
}
