package nuts.project.commerce.application.port.repository

import nuts.project.commerce.domain.stock.StockReservation
import java.util.UUID

interface StockReservationRepository {
    fun save(stockReservation: StockReservation) : StockReservation
    fun findByOrderId(orderId : UUID) : List<StockReservation>
}