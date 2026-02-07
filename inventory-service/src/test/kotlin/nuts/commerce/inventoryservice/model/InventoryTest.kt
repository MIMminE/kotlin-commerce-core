package nuts.commerce.inventoryservice.model

import nuts.commerce.inventoryservice.exception.InventoryException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InventoryTest {

    @Test
    fun `생성 시 기본 상태는 AVAILABLE 이어야 한다`() {
        val inventory = Inventory.create(
            productId = UUID.randomUUID(),
            quantity = 10L
        )

        assertEquals(InventoryStatus.AVAILABLE, inventory.status)
    }

    @Test
    fun `수량 증가 성공`() {
        val inventory = Inventory.create(
            productId = UUID.randomUUID(),
            quantity = 10L
        )

        inventory.increaseQuantity(5L)
        assertEquals(15L, inventory.quantity)
    }

    @Test
    fun `수량 증가 실패 - 음수 입력`() {
        val inventory = Inventory.create(
            productId = UUID.randomUUID(),
            quantity = 10L
        )

        assertFailsWith<InventoryException.InvalidCommand> { inventory.increaseQuantity(-1L) }
    }

    @Test
    fun `수량 감소 성공`() {
        val inventory = Inventory.create(
            productId = UUID.randomUUID(),
            quantity = 10L
        )

        inventory.decreaseQuantity(4L)
        assertEquals(6L, inventory.quantity)
    }

    @Test
    fun `수량 감소 실패 - 음수 입력`() {
        val inventory = Inventory.create(
            productId = UUID.randomUUID(),
            quantity = 10L
        )

        assertFailsWith<InventoryException.InvalidCommand> { inventory.decreaseQuantity(-2L) }
    }

    @Test
    fun `수량 감소 실패 - 재고 부족시 InsufficientInventory 발생`() {
        val inventoryId = UUID.randomUUID()
        val inventory = Inventory.create(
            inventoryId = inventoryId,
            productId = UUID.randomUUID(),
            quantity = 3L
        )

        val ex = assertFailsWith<InventoryException.InsufficientInventory> { inventory.decreaseQuantity(5L) }
        assertTrue(
            ex.message!!.contains("requested") || ex.message!!.contains("insufficient") || ex.message!!.contains(
                "available"
            )
        )
    }

    @Test
    fun `상태 unavailable으로 전환 성공`() {
        val inventory = Inventory.create(productId = UUID.randomUUID(), quantity = 1L)
        inventory.unavailable()
        assertEquals(InventoryStatus.UNAVAILABLE, inventory.status)
    }

    @Test
    fun `상태 unavailable 전이는 이미 UNAVAILABLE이면 예외 발생`() {
        val inventory = Inventory.create(productId = UUID.randomUUID(), quantity = 1L)
        inventory.unavailable()
        assertFailsWith<InventoryException.InvalidTransition> { inventory.unavailable() }
    }

    @Test
    fun `UNAVAILABLE 상태에서 available로 전환 성공`() {
        val inventory = Inventory.create(productId = UUID.randomUUID(), quantity = 1L)
        inventory.unavailable()
        inventory.available()
        assertEquals(InventoryStatus.AVAILABLE, inventory.status)
    }

    @Test
    fun `delete 호출시 상태가 DELETED로 변경된다`() {
        val inventory = Inventory.create(productId = UUID.randomUUID(), quantity = 1L)
        inventory.delete()
        assertEquals(InventoryStatus.DELETED, inventory.status)
    }

    @Test
    fun `delete 전이에서 이미 DELETED면 예외 발생`() {
        val inventory = Inventory.create(productId = UUID.randomUUID(), quantity = 1L)
        inventory.delete()
        assertFailsWith<InventoryException.InvalidTransition> { inventory.delete() }
    }
}