package nuts.commerce.orderservice.adapter.controller

import nuts.commerce.orderservice.usecase.OrderCreateCommand
import nuts.commerce.orderservice.usecase.OrderCreateUseCase
import nuts.commerce.orderservice.usecase.GetOrdersUseCase
import nuts.commerce.orderservice.usecase.Item
import nuts.commerce.orderservice.exception.OrderException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderCreateUseCase: OrderCreateUseCase,
    private val getOrdersUseCase: GetOrdersUseCase
) {
    @PostMapping
    fun create(@RequestBody req: CreateOrderRequest): ResponseEntity<CreateOrderResponse> {
        val idempotencyKey = req.idempotencyKey
            ?: throw OrderException.InvalidCommand("idempotencyKey is required and must be provided by client")

        val orderCreateCommand = OrderCreateCommand(
            idempotencyKey = idempotencyKey,
            userId = req.userId,
            items = req.items.map {
                Item(
                    UUID.fromString(it.productId),
                    it.qty,
                    it.unitPriceAmount,
                    it.unitPriceCurrency
                )
            },
            totalAmount = req.totalAmount,
            currency = req.currency
        )

        val res = orderCreateUseCase.create(orderCreateCommand)
        val location = URI.create("/api/orders/${res.orderId}")
        return ResponseEntity.created(location).body(CreateOrderResponse(res.orderId))
    }

    @GetMapping
    fun list(
        @RequestParam userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageImpl<OrderSummary>> {
        val pageable = PageRequest.of(page, size)
        val pageRes = getOrdersUseCase.get(userId, pageable)
        val summaries =
            // Order 엔티티의 필드명에 맞춰 매핑(orderId, totalPrice)
            pageRes.content.map { OrderSummary(it.orderId, it.userId, it.status.name, it.totalPrice.amount, it.totalPrice.currency) }
        val resultPage = PageImpl(summaries, pageable, pageRes.totalElements)
        return ResponseEntity.ok(resultPage)
    }

}

data class CreateOrderRequest(
    val idempotencyKey: UUID? = null,
    val userId: String,
    val items: List<ItemRequest>,
    val totalAmount: Long,
    val currency: String
)

data class ItemRequest(
    val productId: String,
    val qty: Long,
    val unitPriceAmount: Long,
    val unitPriceCurrency: String
)

data class CreateOrderResponse(val orderId: UUID)

data class OrderSummary(
    val orderId: UUID,
    val userId: String,
    val status: String,
    val totalAmount: Long,
    val currency: String
)