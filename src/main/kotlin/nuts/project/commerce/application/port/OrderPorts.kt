package nuts.project.commerce.application.port

import nuts.project.commerce.domain.order.Order
import java.util.UUID

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: UUID): Order?
}

interface ProductPricingPort {
    fun getUnitPrices(productIds: List<UUID>): List<ProductPrice>
}

interface CouponPolicyPort {
    fun calculateDiscount(userId: UUID, couponId: UUID, originalAmount: Long): CouponDiscountResult
}

data class ProductPrice(val productId: UUID, val unitPrice: Long)

data class CouponDiscountResult(val couponId: UUID, val discountAmount: Long)