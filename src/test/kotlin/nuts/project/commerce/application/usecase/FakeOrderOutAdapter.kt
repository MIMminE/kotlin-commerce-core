package nuts.project.commerce.application.usecase

import nuts.project.commerce.application.port.coupon.CouponDiscountResult
import nuts.project.commerce.application.port.coupon.CouponPolicyPort
import nuts.project.commerce.application.port.repository.OrderRepositoryPort
import nuts.project.commerce.application.port.product.ProductPrice
import nuts.project.commerce.application.port.product.ProductQueryPort
import nuts.project.commerce.domain.order.Order
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeOrderRepositoryPort : OrderRepositoryPort {
    private val store = ConcurrentHashMap<UUID, Order>()

    override fun save(order: Order): Order {
        store[order.id] = order
        return order
    }

    override fun findById(id: UUID): Order? = store[id]
}

class FakeProductQueryPort(
    private val priceByProductId: Map<UUID, Long>
) : ProductQueryPort {
    override fun getUnitPrices(productIds: List<UUID>): List<ProductPrice> =
        productIds.distinct().map { pid ->
            val price = priceByProductId[pid]
                ?: throw IllegalArgumentException("price not found for productId=$pid")
            ProductPrice(pid, price)
        }
}

class FakeCouponPolicyPort(
    private val discountAmount: Long
) : CouponPolicyPort {
    override fun calculateDiscount(userId: UUID, couponId: UUID, originalAmount: Long): CouponDiscountResult {
        // 단순화: 무조건 discountAmount 적용 (원하면 originalAmount 기반 로직으로 확장)
        return CouponDiscountResult(couponId = couponId, discountAmount = discountAmount)
    }
}