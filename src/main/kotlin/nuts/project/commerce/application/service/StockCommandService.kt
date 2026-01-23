package nuts.project.commerce.application.service

import jakarta.transaction.Transactional
import nuts.project.commerce.application.port.repository.StockRepository
import nuts.project.commerce.application.port.repository.StockReservationRepository
import nuts.project.commerce.domain.stock.Stock
import nuts.project.commerce.domain.stock.StockReservation
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class StockCommandService(
    private val stockRepository: StockRepository,
    private val stockReservationRepository: StockReservationRepository
) {

    fun save(stock: Stock): Stock {
        return stockRepository.save(stock)
    }

    @Transactional
    fun reserve(orderId: UUID, productId: UUID, quantity: Long, reservedUntil: Long) {

        val stock = stockRepository.findByProductId(productId)
            ?: throw IllegalArgumentException("Stock not found for productId: $productId")

        stock.reserve(quantity)

        val stockReservation = StockReservation.create(
            orderId = orderId,
            productId = productId,
            quantity = quantity,
            reservedUntil = reservedUntil
        )

        stockReservationRepository.save(stockReservation)
    }

    @Transactional
    fun confirm(orderId: UUID) {
        stockReservationRepository.findByOrderId(orderId).forEach { reservation ->
            val stock = stockRepository.findByProductId(reservation.productId)
                ?: throw IllegalArgumentException("Stock not found for productId: ${reservation.productId}")

            reservation.confirm()
            stock.confirm(reservation.quantity)

            stockRepository.save(stock)
            stockReservationRepository.save(reservation)
        }
    }

    fun release(orderId: UUID) {
        stockReservationRepository.findByOrderId(orderId).forEach { reservation ->
            val stock = stockRepository.findByProductId(reservation.productId)
                ?: throw IllegalArgumentException("Stock not found for productId: ${reservation.productId}")

            reservation.release()
            stock.release(reservation.quantity)

            stockRepository.save(stock)
            stockReservationRepository.save(reservation)
        }
    }
}