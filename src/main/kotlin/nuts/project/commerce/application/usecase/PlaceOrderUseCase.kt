package nuts.project.commerce.application.usecase

import nuts.project.commerce.application.port.query.ProductQuery
import nuts.project.commerce.application.port.command.OrderCommand
import nuts.project.commerce.application.service.CouponQueryService
import nuts.project.commerce.application.service.StockCommandService
import nuts.project.commerce.application.usecase.dto.PlaceOrderCommand
import nuts.project.commerce.application.usecase.dto.PlaceOrderResult
import nuts.project.commerce.domain.order.Order
import nuts.project.commerce.domain.product.StockHandlingPolicy
import java.util.UUID


/**
 * 재고 관련 기능이 추가되어야 한다.
 * - 주문된 상품들 중에 재고 처리 방식이 예약 방식일 경우에
 * - 재고에 예약을 걸어두는 로직이 수행되어야 한다.
 */

class PlaceOrderUseCase(
    private val orderCommand: OrderCommand,
    private val productQuery: ProductQuery,
    private val couponQueryService: CouponQueryService,
    private val stockCommandService: StockCommandService
) {

    fun place(command: PlaceOrderCommand): PlaceOrderResult {

        require(command.items.isNotEmpty()) { "Order must have at least one item." }

        val order = Order.create(command.userId)

        command.items
            .map { item ->
                val product = productQuery.getProduct(item.productId)
                    ?: throw IllegalArgumentException("Product not found: ${item.productId}")
                item to product
            }
            .filter { (_, product) -> product.stockHandlingPolicy == StockHandlingPolicy.RESERVE_THEN_DEDUCT }
            .forEach { (item, _) ->
                stockCommandService.reserve(
                    orderId = order.id,
                    productId = item.productId,
                    quantity = item.qty,
                    reservedUntil = 60L
                )
            }

        val prices = productQuery
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

        applyCouponIfPresent(order, command.couponId)

        val savedOrder = orderCommand.save(order)

        return PlaceOrderResult(
            orderId = savedOrder.id,
            originalAmount = savedOrder.originalAmount,
            discountAmount = savedOrder.discountAmount,
            finalAmount = savedOrder.finalAmount
        )
    }

    private fun applyCouponIfPresent(order: Order, couponId: UUID?) {
        if (couponId == null) return

        val coupon = couponQueryService.findCoupon(couponId)

        require(coupon.canApply(order.originalAmount)) {
            "Coupon cannot be applied: $couponId"
        }

        order.applyDiscount(
            discountAmount = coupon.calculateDiscount(order.originalAmount),
            couponId = couponId
        )
    }
}