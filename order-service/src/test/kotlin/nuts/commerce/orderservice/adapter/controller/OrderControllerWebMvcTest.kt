package nuts.commerce.orderservice.adapter.controller

import nuts.commerce.orderservice.usecase.OrderCreateUseCase
import nuts.commerce.orderservice.usecase.GetOrdersUseCase
import nuts.commerce.orderservice.model.Money
import nuts.commerce.orderservice.model.Order
import nuts.commerce.orderservice.model.OrderStatus
import nuts.commerce.orderservice.usecase.OrderCreateResult
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Suppress("NonAsciiCharacters")
@WebMvcTest(OrderController::class)
class OrderControllerWebMvcTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @field:MockitoBean
    lateinit var orderCreateUseCase: OrderCreateUseCase

    @field:MockitoBean
    lateinit var getOrdersUseCase: GetOrdersUseCase

    @Test
    fun `POST 주문 생성 - 성공`() {
        // given
        val idempotencyKey = UUID.randomUUID()
        val userId = "user-123"
        val productId = UUID.randomUUID()

        val req = CreateOrderRequest(
            idempotencyKey = idempotencyKey,
            userId = userId,
            items = listOf(
                ItemRequest(
                    productId = productId.toString(),
                    qty = 2,
                    unitPriceAmount = 10000,
                    unitPriceCurrency = "KRW"
                )
            ),
            totalAmount = 20000,
            currency = "KRW"
        )

        val orderId = UUID.randomUUID()
        val createResult = OrderCreateResult(orderId)

        whenever(orderCreateUseCase.create(any()))
            .thenReturn(createResult)

        // when & then
        mockMvc.perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.orderId").value(orderId.toString()))

        verify(orderCreateUseCase).create(any())
    }

    @Test
    fun `POST 주문 생성 - idempotencyKey 없으면 실패`() {
        // given
        val userId = "user-456"
        val productId = UUID.randomUUID()

        val req = CreateOrderRequest(
            idempotencyKey = null,  // idempotencyKey 없음
            userId = userId,
            items = listOf(
                ItemRequest(
                    productId = productId.toString(),
                    qty = 1,
                    unitPriceAmount = 5000,
                    unitPriceCurrency = "KRW"
                )
            ),
            totalAmount = 5000,
            currency = "KRW"
        )

        // when & then
        mockMvc.perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET 주문 목록 조회 - 성공`() {
        // given
        val userId = "user-789"
        val page = 0
        val size = 20

        val order1 = Order.create(
            orderId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            userId = userId,
            status = OrderStatus.CREATED
        )
        order1.totalPrice = Money(20000, "KRW")

        val order2 = Order.create(
            orderId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            userId = userId,
            status = OrderStatus.PAYING
        )
        order2.totalPrice = Money(30000, "KRW")

        val orders = listOf(order1, order2)
        val pageResult = PageImpl(orders, PageRequest.of(page, size), 2)

        whenever(getOrdersUseCase.get(userId, PageRequest.of(page, size)))
            .thenReturn(pageResult)

        // when & then
        mockMvc.perform(
            get("/api/orders")
                .param("userId", userId)
                .param("page", page.toString())
                .param("size", size.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.numberOfElements").value(2))
            .andExpect(jsonPath("$.content", hasSize<Any>(2)))
            .andExpect(jsonPath("$.content[0].userId").value(userId))
            .andExpect(jsonPath("$.content[0].status").value("CREATED"))
            .andExpect(jsonPath("$.content[0].totalAmount").value(20000))
            .andExpect(jsonPath("$.content[1].userId").value(userId))
            .andExpect(jsonPath("$.content[1].status").value("PAYING"))
            .andExpect(jsonPath("$.content[1].totalAmount").value(30000))

        verify(getOrdersUseCase).get(userId, PageRequest.of(page, size))
    }

    @Test
    fun `GET 주문 목록 조회 - 빈 결과`() {
        // given
        val userId = "user-empty"
        val page = 0
        val size = 20

        val emptyPageResult = PageImpl<Order>(emptyList(), PageRequest.of(page, size), 0)

        whenever(getOrdersUseCase.get(userId, PageRequest.of(page, size)))
            .thenReturn(emptyPageResult)

        // when & then
        mockMvc.perform(
            get("/api/orders")
                .param("userId", userId)
                .param("page", page.toString())
                .param("size", size.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.numberOfElements").value(0))
            .andExpect(jsonPath("$.content", hasSize<Any>(0)))
            .andExpect(jsonPath("$.totalElements").value(0))

        verify(getOrdersUseCase).get(userId, PageRequest.of(page, size))
    }

    @Test
    fun `GET 주문 목록 조회 - 기본 페이징 옵션`() {
        // given
        val userId = "user-default-page"

        val order = Order.create(
            orderId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            userId = userId,
            status = OrderStatus.COMPLETED
        )
        order.totalPrice = Money(50000, "KRW")

        val pageResult = PageImpl(listOf(order), PageRequest.of(0, 20), 1)

        whenever(getOrdersUseCase.get(userId, PageRequest.of(0, 20)))
            .thenReturn(pageResult)

        // when & then - page와 size를 지정하지 않으면 기본값 사용
        mockMvc.perform(
            get("/api/orders")
                .param("userId", userId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.numberOfElements").value(1))
            .andExpect(jsonPath("$.content[0].status").value("COMPLETED"))
            .andExpect(jsonPath("$.content[0].totalAmount").value(50000))

        verify(getOrdersUseCase).get(userId, PageRequest.of(0, 20))
    }

    @Test
    fun `POST 주문 생성 - 다중 상품`() {
        // given
        val idempotencyKey = UUID.randomUUID()
        val userId = "user-multi-items"
        val productId1 = UUID.randomUUID()
        val productId2 = UUID.randomUUID()

        val req = CreateOrderRequest(
            idempotencyKey = idempotencyKey,
            userId = userId,
            items = listOf(
                ItemRequest(
                    productId = productId1.toString(),
                    qty = 2,
                    unitPriceAmount = 10000,
                    unitPriceCurrency = "KRW"
                ),
                ItemRequest(
                    productId = productId2.toString(),
                    qty = 3,
                    unitPriceAmount = 5000,
                    unitPriceCurrency = "KRW"
                )
            ),
            totalAmount = 35000,
            currency = "KRW"
        )

        val orderId = UUID.randomUUID()
        val createResult = OrderCreateResult(orderId)

        whenever(orderCreateUseCase.create(any()))
            .thenReturn(createResult)

        // when & then
        mockMvc.perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.orderId").value(orderId.toString()))

        verify(orderCreateUseCase).create(any())
    }

    @Test
    fun `GET 주문 목록 조회 - 페이지 2 조회`() {
        // given
        val userId = "user-page-2"
        val page = 1
        val size = 10

        val orders = (1..10).map { index ->
            Order.create(
                orderId = UUID.randomUUID(),
                idempotencyKey = UUID.randomUUID(),
                userId = userId,
                status = OrderStatus.COMPLETED
            ).apply {
                totalPrice = Money((index * 10000).toLong(), "KRW")
            }
        }

        val pageResult = PageImpl(orders, PageRequest.of(page, size), 25)

        whenever(getOrdersUseCase.get(userId, PageRequest.of(page, size)))
            .thenReturn(pageResult)

        // when & then
        mockMvc.perform(
            get("/api/orders")
                .param("userId", userId)
                .param("page", page.toString())
                .param("size", size.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.numberOfElements").value(10))
            .andExpect(jsonPath("$.content", hasSize<Any>(10)))
            .andExpect(jsonPath("$.totalElements").value(25))
            .andExpect(jsonPath("$.number").value(page))

        verify(getOrdersUseCase).get(userId, PageRequest.of(page, size))
    }

}