package nuts.commerce.orderservice.model.domain

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OrderSagaTest {

    @Test
    fun `create 정상`() {
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(orderId) // oops: create method name is create(orderId,...)
        assertEquals(orderId, saga.orderId)
    }

    @Test
    fun `전이 시나리오`() {
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(orderId)
        saga.markInventoryReserved()
        assertEquals(OrderSaga.SagaStatus.INVENTORY_RESERVED, saga.status)

        saga.markPaymentRequested()
        assertEquals(OrderSaga.SagaStatus.PAYMENT_REQUESTED, saga.status)

        saga.markPaymentCompleted()
        assertEquals(OrderSaga.SagaStatus.PAYMENT_COMPLETED, saga.status)

        saga.markCompleted()
        assertEquals(OrderSaga.SagaStatus.COMPLETED, saga.status)
    }

    @Test
    fun `invalid transition throws`() {
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(orderId)
        assertFailsWith<Exception> { saga.markPaymentRequested() }
    }
}
