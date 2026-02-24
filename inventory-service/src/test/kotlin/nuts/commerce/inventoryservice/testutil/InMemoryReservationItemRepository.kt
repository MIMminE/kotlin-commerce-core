package nuts.commerce.inventoryservice.testutil

import nuts.commerce.inventoryservice.model.ReservationItem
import nuts.commerce.inventoryservice.port.repository.ReservationItemRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryReservationItemRepository : ReservationItemRepository {
    private val store = ConcurrentHashMap<UUID, ReservationItem>()
    private val reservationIdIndex = ConcurrentHashMap<UUID, MutableList<UUID>>()

    override fun save(items: List<ReservationItem>): List<ReservationItem> {
        val savedItems = mutableListOf<ReservationItem>()

        items.forEach { item ->
            store[item.id] = item

            // 인덱싱: reservationId별 item id 매핑
            val reservationId = item.reservationId ?: return@forEach
            reservationIdIndex
                .computeIfAbsent(reservationId) { mutableListOf() }
                .add(item.id)

            savedItems.add(item)
        }

        return savedItems
    }

    override fun findByReservationId(reservationId: UUID): List<ReservationItem> {
        val itemIds = reservationIdIndex[reservationId] ?: return emptyList()
        return itemIds.mapNotNull { store[it] }
    }

    fun clear() {
        store.clear()
        reservationIdIndex.clear()
    }

    fun getAll(): List<ReservationItem> {
        return store.values.toList()
    }
}