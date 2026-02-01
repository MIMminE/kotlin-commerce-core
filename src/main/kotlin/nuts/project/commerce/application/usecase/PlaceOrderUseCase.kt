package nuts.project.commerce.application.usecase

import nuts.project.commerce.application.service.CouponService
import nuts.project.commerce.application.service.IdempotencyService
import nuts.project.commerce.application.service.OrderService
import nuts.project.commerce.application.service.ProductService
import nuts.project.commerce.application.service.StartResult
import nuts.project.commerce.application.service.StockService
import nuts.project.commerce.application.usecase.dto.PlaceOrderCommand
import nuts.project.commerce.application.usecase.dto.PlaceOrderResult
import nuts.project.commerce.domain.common.Idempotency
import nuts.project.commerce.domain.common.Idempotency.ActionType
import nuts.project.commerce.domain.core.Order
import java.util.UUID

class PlaceOrderUseCase(
    private val orderService: OrderService,
    private val stockService: StockService,
    private val couponService: CouponService,
    private val productService: ProductService,
    private val idempotencyService: IdempotencyService
) {

    fun place(command: PlaceOrderCommand): PlaceOrderResult {

        require(command.items.isNotEmpty()) { "Order must have at least one item." }

        val result = idempotencyService.tryStart(
            scopeId = command.userId,
            action = ActionType.PLACE_ORDER,
            idemKey = command.commandIdempotencyKey
        )

        when (result) {
            is StartResult.Existing -> {
                result.resourceId?.let {
                    val order = orderService.findById(result.resourceId)
                    return PlaceOrderResult(
                        orderId = order.id,
                        originalAmount = order.originalAmount,
                        discountAmount = order.discountAmount,
                        finalAmount = order.finalAmount
                    )
                }
            }

            is StartResult.Started -> {
                println("Idempotent operation started: id=${result.id}, createdAt=${result.createdAt}")
            }
        }

        val orderItems = command.items.associateBy { it.productId }

        val order = Order.create(command.userId)

        val products = productService.getProducts(command.items.map { it.productId })

        products
            .filter { it.stockHandlingPolicy == StockHandlingPolicy.RESERVE_THEN_DEDUCT }
            .forEach { product ->
                val item = command.items.find { it.productId == product.id }!!
                stockService.reserve(
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

        val savedOrder = orderService.save(order)

        return PlaceOrderResult(
            orderId = savedOrder.id,
            originalAmount = savedOrder.originalAmount,
            discountAmount = savedOrder.discountAmount,
            finalAmount = savedOrder.finalAmount
        )
    }

    private fun applyCouponIfPresent(order: Order, couponId: UUID?) {
        if (couponId == null) return

        val coupon = couponService.getValidCoupon(couponId, order.originalAmount)
        order.applyDiscount(
            discountAmount = coupon.calculateDiscount(order.originalAmount),
            couponId = couponId
        )
    }
}