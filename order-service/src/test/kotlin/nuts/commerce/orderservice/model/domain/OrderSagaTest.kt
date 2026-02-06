package nuts.commerce.orderservice.model.domain

import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import nuts.commerce.orderservice.model.exception.OrderException

class OrderSagaTest {

    @Test
    fun `create 정상`() {
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(orderId)
        assertEquals(orderId, saga.orderId)
        // 생성 직후에는 모든 타임스탬프가 null 이어야 함
        assertNull(saga.orderEventReceivedAt)
        assertNull(saga.inventoryRequestedAt)
        assertNull(saga.inventoryReservedAt)
        assertNull(saga.paymentRequestedAt)
        assertNull(saga.paymentCompletedAt)
        assertNull(saga.inventoryReleasedAt)
        assertNull(saga.completedAt)
        assertNull(saga.failedAt)
    }

    @Test
    fun `주문_이벤트_수신하면_타임스탬프가_설정된다`() {
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(orderId)
        val ts = Instant.parse("2020-01-01T00:00:00Z")
        saga.markOrderEventReceived(ts)
        assertEquals(ts, saga.orderEventReceivedAt)
    }

    @Test
    fun `인벤토리_요청하면_상태가_INVENTORY_REQUESTED_되고_타임스탬프_설정된다`() {
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(orderId)
        val ts = Instant.parse("2020-01-01T01:00:00Z")
        saga.markInventoryRequested(ts)
        assertEquals(OrderSaga.SagaStatus.INVENTORY_REQUESTED, saga.status)
        assertEquals(ts, saga.inventoryRequestedAt)
    }

    @Test
    fun `인벤토리_예약하면_상태가_INVENTORY_RESERVED_되고_타임스탬프_설정된다`() {
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(orderId)
        val ts = Instant.parse("2020-01-01T02:00:00Z")
        saga.markInventoryReserved(ts)
        assertEquals(OrderSaga.SagaStatus.INVENTORY_RESERVED, saga.status)
        assertEquals(ts, saga.inventoryReservedAt)
    }

    @Test
    fun `결제_요청하면_상태가_PAYMENT_REQUESTED_되고_타임스탬프_설정된다`() {
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(orderId)
        saga.markInventoryReserved()
        val ts = Instant.parse("2020-01-01T03:00:00Z")
        saga.markPaymentRequested(ts)
        assertEquals(OrderSaga.SagaStatus.PAYMENT_REQUESTED, saga.status)
        assertEquals(ts, saga.paymentRequestedAt)
    }

    @Test
    fun `결제_완료하면_상태가_PAYMENT_COMPLETED_되고_타임스탬프_설정된다`() {
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(orderId)
        saga.markInventoryReserved()
        saga.markPaymentRequested()
        val ts = Instant.parse("2020-01-01T04:00:00Z")
        saga.markPaymentCompleted(ts)
        assertEquals(OrderSaga.SagaStatus.PAYMENT_COMPLETED, saga.status)
        assertEquals(ts, saga.paymentCompletedAt)
    }

    @Test
    fun `인벤토리_해제하면_상태가_INVENTORY_RELEASED_되고_타임스탬프_설정된다`() {
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(orderId)
        val ts = Instant.parse("2020-01-01T05:00:00Z")
        saga.markInventoryReleased(ts)
        assertEquals(OrderSaga.SagaStatus.INVENTORY_RELEASED, saga.status)
        assertEquals(ts, saga.inventoryReleasedAt)
    }

    @Test
    fun `완료하면_상태가_COMPLETED_되고_타임스탬프_설정된다`() {
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(orderId)
        saga.markInventoryReserved()
        saga.markPaymentRequested()
        saga.markPaymentCompleted()
        val ts = Instant.parse("2020-01-01T06:00:00Z")
        saga.markCompleted(ts)
        assertEquals(OrderSaga.SagaStatus.COMPLETED, saga.status)
        assertEquals(ts, saga.completedAt)
    }

    @Test
    fun `실패하면_failedAt_설정된다`() {
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(orderId)
        val ts = Instant.parse("2020-01-01T07:00:00Z")
        saga.fail(ts)
        assertEquals(OrderSaga.SagaStatus.FAILED, saga.status)
        assertEquals(ts, saga.failedAt)
    }

    @Test
    fun `invalid transition throws`() {
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(orderId)
        // CREATED 상태에서 바로 결제 요청은 허용되지 않음
        assertFailsWith<OrderException.InvalidTransition> { saga.markPaymentRequested() }
    }

    @Test
    fun `중복_인벤토리_요청시_InvalidTransition_던짐`() {
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(orderId)
        saga.markInventoryRequested()
        assertFailsWith<OrderException.InvalidTransition> { saga.markInventoryRequested() }
    }
}
