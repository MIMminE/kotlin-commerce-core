package nuts.project.commerce.domain.order

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class OrderAggregateTest {
    private fun uuid() = UUID.randomUUID()

    // -----------------------------
    // create()
    // -----------------------------

    @Test
    fun `create - 생성 직후 기본 상태는 CREATED이고 금액은 0`() {
        val userId = uuid()

        val order = Order.create(userId)

        assertEquals(userId, order.userId)
        assertEquals(OrderStatus.CREATED, order.status)
        assertEquals(0L, order.originalAmount)
        assertEquals(0L, order.discountAmount)
        assertEquals(0L, order.finalAmount)
        assertNull(order.appliedCouponId)
        assertEquals(0, order.items.size)
    }

    // -----------------------------
    // addItem()
    // -----------------------------

    @Test
    fun `addItem - CREATED 상태에서 아이템 추가 가능`() {
        val order = Order.create(uuid())
        val productId = uuid()

        order.addItem(productId = productId, qty = 2, unitPriceSnapshot = 1000)

        assertEquals(1, order.items.size)
        val item = order.items[0]
        assertEquals(productId, item.productId)
        assertEquals(2L, item.qty)
        assertEquals(1000L, item.unitPriceSnapshot)
    }

    @Test
    fun `addItem - qty가 0 이하이면 예외`() {
        val order = Order.create(uuid())

        assertThrows<IllegalArgumentException> {
            order.addItem(productId = uuid(), qty = 0, unitPriceSnapshot = 1000)
        }
    }

    @Test
    fun `addItem - unitPriceSnapshot이 음수면 예외`() {
        val order = Order.create(uuid())

        assertThrows<IllegalArgumentException> {
            order.addItem(productId = uuid(), qty = 1, unitPriceSnapshot = -1)
        }
    }

    @Test
    fun `addItem - PAID 상태에서는 아이템 추가 불가`() {
        val order = Order.create(uuid())
        order.addItem(productId = uuid(), qty = 1, unitPriceSnapshot = 1000)
        order.applyDiscount(discountAmount = 0, couponId = null) // finalAmount 0 유지 목적 아님, 그냥 흐름 예시
        order.markPaid()

        assertThrows<IllegalArgumentException> {
            order.addItem(productId = uuid(), qty = 1, unitPriceSnapshot = 1000)
        }
    }

    @Test
    fun `addItem - FAILED 상태에서는 아이템 추가 불가`() {
        val order = Order.create(uuid())
        order.markFailed()

        assertThrows<IllegalArgumentException> {
            order.addItem(productId = uuid(), qty = 1, unitPriceSnapshot = 1000)
        }
    }

    @Test
    fun `addItem - (목표 테스트) 아이템 추가 시 금액이 재계산되어야 한다`() {
        val order = Order.create(uuid())

        order.addItem(productId = uuid(), qty = 2, unitPriceSnapshot = 1000) // +2000
        order.addItem(productId = uuid(), qty = 1, unitPriceSnapshot = 500)  // +500

        assertEquals(2500L, order.originalAmount)
        assertEquals(0L, order.discountAmount)
        assertEquals(2500L, order.finalAmount)
    }

    // -----------------------------
    // applyDiscount()
    // -----------------------------

    @Test
    fun `applyDiscount - CREATED 상태에서만 가능`() {
        val order = Order.create(uuid())
        order.markFailed()

        assertThrows<IllegalArgumentException> {
            order.applyDiscount(discountAmount = 100, couponId = uuid())
        }
    }

    @Test
    fun `applyDiscount - discountAmount 음수면 예외`() {
        val order = Order.create(uuid())

        assertThrows<IllegalArgumentException> {
            order.applyDiscount(discountAmount = -1, couponId = uuid())
        }
    }

    @Test
    fun `applyDiscount - discountAmount는 originalAmount를 넘지 않도록 clamp 된다`() {
        val order = Order.create(uuid())
        order.addItem(productId = uuid(), qty = 1, unitPriceSnapshot = 1000)

        order.applyDiscount(discountAmount = 9999, couponId = uuid())

        assertEquals(1000L, order.originalAmount)
        assertEquals(1000L, order.discountAmount)
        assertEquals(0L, order.finalAmount)
    }

    @Test
    fun `applyDiscount - couponId가 반영된다`() {
        val order = Order.create(uuid())
        order.addItem(productId = uuid(), qty = 1, unitPriceSnapshot = 1000)
        val couponId = uuid()

        order.applyDiscount(discountAmount = 100, couponId = couponId)

        assertEquals(couponId, order.appliedCouponId)
        assertEquals(900L, order.finalAmount)
    }

    // -----------------------------
    // markPaid() & markFailed()
    // -----------------------------

    @Test
    fun `markPaid - CREATED 상태에서만 가능`() {
        val order = Order.create(uuid())
        order.markFailed()

        assertThrows<IllegalArgumentException> {
            order.markPaid()
        }
    }

    @Test
    fun `markPaid - 상태가 PAID로 변경된다`() {
        val order = Order.create(uuid())
        // finalAmount >= 0 조건을 만족시키기 위해 (기본 0이라 그냥 통과)
        order.markPaid()

        assertEquals(OrderStatus.PAID, order.status)
    }

    @Test
    fun `markFailed - CREATED 상태에서만 가능`() {
        val order = Order.create(uuid())
        order.markPaid()

        assertThrows<IllegalArgumentException> {
            order.markFailed()
        }
    }

    @Test
    fun `markFailed - 상태가 FAILED로 변경된다`() {
        val order = Order.create(uuid())

        order.markFailed()

        assertEquals(OrderStatus.FAILED, order.status)
    }
}