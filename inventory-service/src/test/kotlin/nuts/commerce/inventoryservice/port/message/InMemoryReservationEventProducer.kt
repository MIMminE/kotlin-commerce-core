package nuts.commerce.inventoryservice.port.message

import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryReservationEventProducer : ReservationEventProducer {
    private val events: ConcurrentHashMap<UUID, CopyOnWriteArrayList<QuantityUpdateEvent>> = ConcurrentHashMap()

    override fun produce(inventoryId: UUID, event: QuantityUpdateEvent): CompletableFuture<Unit> {
        val list = events.computeIfAbsent(inventoryId) { CopyOnWriteArrayList() }
        list.add(event)
        return CompletableFuture.completedFuture(Unit)
    }

    fun producedFor(inventoryId: UUID): List<QuantityUpdateEvent> = events[inventoryId]?.toList() ?: emptyList()

    fun producedAll(): Map<UUID, List<QuantityUpdateEvent>> = events.mapValues { it.value.toList() }

    fun clear() {
        events.clear()
    }
}

