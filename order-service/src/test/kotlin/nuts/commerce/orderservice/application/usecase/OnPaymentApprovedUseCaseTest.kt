//package nuts.commerce.orderservice.application.usecase
//
//import nuts.commerce.orderservice.application.port.repository.InMemoryOrderRepository
//import nuts.commerce.orderservice.utils.FixtureUtils.orderFixtureCreated
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.BeforeEach
//import java.util.UUID
//import kotlin.test.Test
//
//class OnPaymentApprovedUseCaseTest {
//
//    private val orderRepository = InMemoryOrderRepository()
//    private val useCase = OnPaymentApprovedUseCase(orderRepository)
//
//    @BeforeEach
//    fun setUp() {
//        orderRepository.clear()
//    }
//
//    @Test
//    fun `주문이 없으면 이벤트를 무시하고 예외 없이 종료한다`() {
//        val event = OnPaymentApprovedUseCase.PaymentApprovedEvent(
//            eventId = "evt-1",
//            orderId = UUID.randomUUID(),
//            paymentId = "pay-1",
//        )
//
//        assertDoesNotThrow { useCase.handle(event) }
//    }
//
//    @Test
//    fun `결제 승인 이벤트 처리 시 주문 상태가 PAID로 변경되고 저장된다`() {
//        // given
//        val orderId = UUID.randomUUID()
//        val order = orderFixtureCreated(id = orderId, userId = "user-1")
//        orderRepository.save(order)
//
//        // when
//        useCase.handle(
//            OnPaymentApprovedUseCase.PaymentApprovedEvent(
//                eventId = "evt-1",
//                orderId = orderId,
//                paymentId = "pay-1",
//            )
//        )
//
//        // then
//        val saved = orderRepository.findById(orderId)!!
//        assertEquals("PAID", saved.status.name)
//    }
//
//    @Test
//    fun `이미 PAID인 주문에 중복 결제 승인 이벤트가 오면 no-op이다`() {
//        // given: 이미 PAID 상태 주문
//        val orderId = UUID.randomUUID()
//        val order = orderFixtureCreated(id = orderId, userId = "user-1").apply {
//            markPaying()
//            applyPaymentApproved()
//        }
//        orderRepository.save(order)
//
//        // when
//        useCase.handle(
//            OnPaymentApprovedUseCase.PaymentApprovedEvent(
//                eventId = "evt-1",
//                orderId = orderId,
//                paymentId = "pay-1",
//            )
//        )
//
//        // then
//        val saved = orderRepository.findById(orderId)!!
//        assertEquals(OrderStatus.PAID, saved.status)
//    }
//}