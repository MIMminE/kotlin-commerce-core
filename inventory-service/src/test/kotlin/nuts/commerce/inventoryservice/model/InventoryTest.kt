//@file:Suppress("NonAsciiCharacters")
//
//package nuts.commerce.inventoryservice.model
//
//import nuts.commerce.inventoryservice.exception.InventoryException
//import java.util.UUID
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertFailsWith
//import kotlin.test.assertTrue
//
//class InventoryTest {
//
//    @Test
//    fun `생성 시 기본 상태는 AVAILABLE 이어야 한다`() {
//        val inventory = Inventory.create(
//            productId = UUID.randomUUID(),
//            availableQuantity = 10L
//        )
//
//        assertEquals(InventoryStatus.AVAILABLE, inventory.status)
//    }
//
//    @Test
//    fun `수량 증가 성공`() {
//        val inventory = Inventory.create(
//            productId = UUID.randomUUID(),
//            availableQuantity = 10L
//        )
//
//        inventory.increaseQuantity(5L)
//        assertEquals(15L, inventory.availableQuantity)
//    }
//
//    @Test
//    fun `수량 증가 실패 - 음수 입력`() {
//        val inventory = Inventory.create(
//            productId = UUID.randomUUID(),
//            availableQuantity = 10L
//        )
//
//        assertFailsWith<InventoryException.InvalidCommand> { inventory.increaseQuantity(-1L) }
//    }
//
//    @Test
//    fun `수량 감소 성공`() {
//        val inventory = Inventory.create(
//            productId = UUID.randomUUID(),
//            availableQuantity = 10L
//        )
//
//        inventory.decreaseQuantity(4L)
//        assertEquals(6L, inventory.quantity)
//    }
//
//    @Test
//    fun `수량 감소 실패 - 음수 입력`() {
//        val inventory = Inventory.create(
//            productId = UUID.randomUUID(),
//            availableQuantity = 10L
//        )
//
//        assertFailsWith<InventoryException.InvalidCommand> { inventory.decreaseQuantity(-2L) }
//    }
//
//    @Test
//    fun `수량 감소 실패 - 재고 부족시 InsufficientInventory 발생`() {
//        val inventoryId = UUID.randomUUID()
//        val inventory = Inventory.create(
//            inventoryId = inventoryId,
//            productId = UUID.randomUUID(),
//            availableQuantity = 3L
//        )
//
//        val ex = assertFailsWith<InventoryException.InsufficientInventory> { inventory.decreaseQuantity(5L) }
//        assertTrue(
//            ex.message!!.contains("requested") || ex.message!!.contains("insufficient") || ex.message!!.contains(
//                "available"
//            )
//        )
//    }
//
//    @Test
//    fun `상태 unavailable으로 전환 성공`() {
//        val inventory = Inventory.create(productId = UUID.randomUUID(), availableQuantity = 1L)
//        inventory.unavailable()
//        assertEquals(InventoryStatus.UNAVAILABLE, inventory.status)
//    }
//
//    @Test
//    fun `상태 unavailable 전이는 이미 UNAVAILABLE이면 예외 발생`() {
//        val inventory = Inventory.create(productId = UUID.randomUUID(), availableQuantity = 1L)
//        inventory.unavailable()
//        assertFailsWith<InventoryException.InvalidTransition> { inventory.unavailable() }
//    }
//
//    @Test
//    fun `UNAVAILABLE 상태에서 available로 전환 성공`() {
//        val inventory = Inventory.create(productId = UUID.randomUUID(), availableQuantity = 1L)
//        inventory.unavailable()
//        inventory.available()
//        assertEquals(InventoryStatus.AVAILABLE, inventory.status)
//    }
//
//    @Test
//    fun `delete 호출시 상태가 DELETED로 변경된다`() {
//        val inventory = Inventory.create(productId = UUID.randomUUID(), availableQuantity = 1L)
//        inventory.delete()
//        assertEquals(InventoryStatus.DELETED, inventory.status)
//    }
//
//    @Test
//    fun `delete 전이에서 이미 DELETED면 예외 발생`() {
//        val inventory = Inventory.create(productId = UUID.randomUUID(), availableQuantity = 1L)
//        inventory.delete()
//        assertFailsWith<InventoryException.InvalidTransition> { inventory.delete() }
//    }
//
//    @Test
//    fun `예약 시 available과 reserved 값이 변경되어야 한다`() {
//        val inventory = Inventory.create(
//            productId = UUID.randomUUID(),
//            availableQuantity = 10L
//        )
//
//        inventory.reserve(4L)
//        assertEquals(6L, inventory.availableQuantity)
//        assertEquals(4L, inventory.reservedQuantity)
//        assertEquals(6L, inventory.quantity)
//    }
//
//    @Test
//    fun `예약 실패 - 음수 입력`() {
//        val inventory = Inventory.create(
//            productId = UUID.randomUUID(),
//            availableQuantity = 10L
//        )
//
//        assertFailsWith<InventoryException.InvalidCommand> { inventory.reserve(-1L) }
//    }
//
//    @Test
//    fun `예약 실패 - 재고 부족`() {
//        val inventory = Inventory.create(
//            productId = UUID.randomUUID(),
//            availableQuantity = 3L
//        )
//
//        assertFailsWith<InventoryException.InsufficientInventory> { inventory.reserve(5L) }
//    }
//
//    @Test
//    fun `예약 해제 시 reserved 감소와 available 증가`() {
//        val inventory = Inventory.create(productId = UUID.randomUUID(), availableQuantity = 10L)
//        inventory.reserve(5L)
//        inventory.unreserve(3L)
//        assertEquals(2L, inventory.reservedQuantity)
//        assertEquals(8L, inventory.availableQuantity)
//    }
//
//    @Test
//    fun `예약 해제 실패 - 음수 입력`() {
//        val inventory = Inventory.create(productId = UUID.randomUUID(), availableQuantity = 10L)
//        inventory.reserve(2L)
//        assertFailsWith<InventoryException.InvalidCommand> { inventory.unreserve(-1L) }
//    }
//
//    @Test
//    fun `예약 해제 실패 - 예약량 부족`() {
//        val inventory = Inventory.create(productId = UUID.randomUUID(), availableQuantity = 2L)
//        inventory.reserve(2L)
//        assertFailsWith<InventoryException.InvalidCommand> { inventory.unreserve(3L) }
//    }
//
//    @Test
//    fun `예약된 물건 처리 시 reserved 감소하고 available은 변하지 않아야 한다`() {
//        val inventory = Inventory.create(productId = UUID.randomUUID(), availableQuantity = 10L)
//        inventory.reserve(5L)
//        inventory.processReserved(4L)
//        assertEquals(1L, inventory.reservedQuantity)
//        assertEquals(5L, inventory.availableQuantity)
//    }
//
//    @Test
//    fun `예약된 물건 처리 실패 - 음수 입력 및 부족`() {
//        val inventory = Inventory.create(productId = UUID.randomUUID(), availableQuantity = 5L)
//        inventory.reserve(3L)
//        assertFailsWith<InventoryException.InvalidCommand> { inventory.processReserved(-1L) }
//        assertFailsWith<InventoryException.InvalidCommand> { inventory.processReserved(5L) }
//    }
//}