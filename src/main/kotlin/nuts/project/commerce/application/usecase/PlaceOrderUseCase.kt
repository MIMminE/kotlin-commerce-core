package nuts.project.commerce.application.usecase

import nuts.project.commerce.application.port.coupon.CouponPolicyPort
import nuts.project.commerce.application.port.repository.OrderRepositoryPort
import nuts.project.commerce.application.port.product.ProductQueryPort
import nuts.project.commerce.domain.order.Order
import java.util.UUID

class PlaceOrderUseCase(
    private val orderRepositoryPort: OrderRepositoryPort,
    private val productQueryPort: ProductQueryPort,
    private val couponPolicyPort: CouponPolicyPort,
    private val inventoryUpdatePort: InventoryUpdatePort
) {

    fun place(command: PlaceOrderCommand): PlaceOrderResult {

        require(command.items.isNotEmpty()) { "Order must have at least one item." }

        val order = Order.create(command.userId)

        val prices = productQueryPort
            .getUnitPrices(command.items.map { it.productId })
            .associateBy { it.productId }

        command.items.forEach { item ->
            val unitPrice = prices[item.productId]?.unitPrice
                ?: throw IllegalArgumentException("Price not found for productId=${item.productId}")

            order.addItem(
                productId = item.productId,
                qty = item.qty,
                unitPriceSnapshot = unitPrice
            )
        }

        val couponId = command.couponId
        if (couponId != null) {
            val discount = couponPolicyPort.calculateDiscount(
                userId = command.userId,
                couponId = couponId,
                originalAmount = order.originalAmount
            )
            order.applyDiscount(discount.discountAmount, discount.couponId)
        }

        val savedOrder = orderRepositoryPort.save(order)

        return PlaceOrderResult(
            orderId = savedOrder.id,
            originalAmount = savedOrder.originalAmount,
            discountAmount = savedOrder.discountAmount,
            finalAmount = savedOrder.finalAmount
        )
    }
}

data class PlaceOrderCommand(
    val userId: UUID,
    val items: List<Item>,
    val couponId: UUID? = null
) {
    data class Item(val productId: UUID, val qty: Long)
}

data class PlaceOrderResult(
    val orderId: UUID,
    val originalAmount: Long,
    val discountAmount: Long,
    val finalAmount: Long
)