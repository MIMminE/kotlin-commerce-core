package nuts.project.commerce.application.usecase

import nuts.project.commerce.application.service.CouponQueryService
import nuts.project.commerce.application.service.OrderCommandService
import nuts.project.commerce.application.service.ProductQueryService
import nuts.project.commerce.application.service.StockCommandService
import nuts.project.commerce.application.usecase.dto.PlaceOrderCommand
import nuts.project.commerce.application.usecase.dto.PlaceOrderResult
import nuts.project.commerce.domain.order.Order
import nuts.project.commerce.domain.product.StockHandlingPolicy
import java.util.UUID

class PlaceOrderUseCase(
    private val orderCommandService: OrderCommandService,
    private val stockCommandService: StockCommandService,
    private val couponQueryService: CouponQueryService,
    private val productQueryService: ProductQueryService
) {

    fun place(command: PlaceOrderCommand): PlaceOrderResult {

        require(command.items.isNotEmpty()) { "Order must have at least one item." }
        val orderItems = command.items.associateBy { it.productId }

        val order = Order.create(command.userId)

        val products = productQueryService.getProducts(command.items.map { it.productId })

        products
            .filter { it.stockHandlingPolicy == StockHandlingPolicy.RESERVE_THEN_DEDUCT }
            .forEach { product ->
                val item = command.items.find { it.productId == product.id }!!
                stockCommandService.reserve(
                    orderId = order.id,
                    productId = item.productId,
                    quantity = item.qty,
                    reservedUntil = 60L
                )
            }

        products.forEach {
            order.addItem(
                productId = it.id,
                qty = orderItems[it.id]!!.qty,
                unitPriceSnapshot = it.price.amount
            )
        }

        applyCouponIfPresent(order, command.couponId)

        val savedOrder = orderCommandService.save(order)

        return PlaceOrderResult(
            orderId = savedOrder.id,
            originalAmount = savedOrder.originalAmount,
            discountAmount = savedOrder.discountAmount,
            finalAmount = savedOrder.finalAmount
        )
    }

    private fun applyCouponIfPresent(order: Order, couponId: UUID?) {
        if (couponId == null) return

        val coupon = couponQueryService.getValidCoupon(couponId, order.originalAmount)
        order.applyDiscount(
            discountAmount = coupon.calculateDiscount(order.originalAmount),
            couponId = couponId
        )
    }
}