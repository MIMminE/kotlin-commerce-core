package nuts.commerce.orderservice.application.adapter.controller

import nuts.commerce.orderservice.application.usecase.CreateOrderUseCase
import nuts.commerce.orderservice.application.usecase.GetOrdersUseCase
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(
    private val createOrderUseCase: CreateOrderUseCase,
    private val getOrdersUseCase: GetOrdersUseCase
) {
}