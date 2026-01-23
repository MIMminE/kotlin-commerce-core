package nuts.project.commerce.port.repository

import nuts.project.commerce.application.port.repository.StockReservationRepository
import nuts.project.commerce.domain.stock.StockReservation
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryStockReservationRepository : StockReservationRepository {

    // orderId -> reservations
    private val storeByOrderId = ConcurrentHashMap<UUID, CopyOnWriteArrayList<StockReservation>>()

    override fun save(stockReservation: StockReservation): StockReservation {
        storeByOrderId
            .computeIfAbsent(stockReservation.orderId) { CopyOnWriteArrayList() }
            .add(stockReservation)

        return stockReservation
    }

    override fun findByOrderId(orderId: UUID): List<StockReservation> =
        storeByOrderId[orderId]?.toList() ?: emptyList()

    fun clear() = storeByOrderId.clear()
}