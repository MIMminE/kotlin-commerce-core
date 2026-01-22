package nuts.project.commerce.application.usecase

import nuts.project.commerce.application.port.command.OrderCommand
import nuts.project.commerce.application.port.dto.ProductPrice
import nuts.project.commerce.application.port.query.CouponQuery
import nuts.project.commerce.application.port.query.OrderQuery
import nuts.project.commerce.application.port.query.ProductQuery
import nuts.project.commerce.application.port.query.StockQuery
import nuts.project.commerce.application.port.repository.CouponRepository
import nuts.project.commerce.application.port.repository.StockRepository
import nuts.project.commerce.application.port.repository.StockReservationRepository
import nuts.project.commerce.domain.coupon.Coupon
import nuts.project.commerce.domain.coupon.CouponType
import nuts.project.commerce.domain.order.Order
import nuts.project.commerce.domain.product.Product
import nuts.project.commerce.domain.stock.Stock
import nuts.project.commerce.domain.stock.StockReservation
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeOrderRepository : OrderCommand, OrderQuery {
    private val store = ConcurrentHashMap<UUID, Order>()

    override fun save(order: Order): Order {
        store[order.id] = order
        return order
    }

    override fun findById(id: UUID): Order? {
        return store[id]
    }
}

class FakeStockRepository(
    private val stockByProductId: Map<UUID, Stock>
) : StockRepository {
    override fun save(stock: Stock): Stock {
        stockByProductId[stock.productId]
            ?: throw IllegalArgumentException("Stock not found for productId: ${stock.productId}")
        return stock
    }

    override fun findByProductId(productId: UUID): Stock? {
        return stockByProductId[productId]
    }
}

class FakeStockReservationRepository(
    private val reservationsByOrderId: MutableMap<UUID, List<StockReservation>> = mutableMapOf()
) : StockReservationRepository {
    override fun save(stockReservation: StockReservation): StockReservation {
        val existingReservations = reservationsByOrderId[stockReservation.orderId] ?: emptyList()
        reservationsByOrderId[stockReservation.orderId] = existingReservations + stockReservation
        return stockReservation
    }

    override fun findByOrderId(orderId: UUID): List<StockReservation> {
        return reservationsByOrderId[orderId] ?: emptyList()
    }
}

class FakeProductQuery(
    private val priceByProductId: Map<UUID, Product>
) : ProductQuery {
    override fun getProduct(productId: UUID): Product {

        return priceByProductId[productId]
            ?: throw IllegalArgumentException("Product not found for productId: $productId")
    }

    override fun getUnitPrices(productIds: List<UUID>): List<ProductPrice> {

        return productIds.map { productId ->
            val product = priceByProductId[productId]
                ?: throw IllegalArgumentException("Product not found for productId: $productId")
            ProductPrice(productId = productId, unitPrice = product.price.amount)
        }
    }
}

class FakeCouponRepository(
    private val couponsById: Map<UUID, Coupon>
) : CouponRepository {

    override fun findById(couponId: UUID): Coupon? {
        return couponsById[couponId]
    }
}

class FakeCouponQuery(
    private val discountAmount: Long
) : CouponQuery {
    override fun findById(id: UUID): Coupon? {
        return Coupon.create(
            type = CouponType.FIXED_AMOUNT,
            value = discountAmount,
            minOrderAmount = 0L
        )
    }
}