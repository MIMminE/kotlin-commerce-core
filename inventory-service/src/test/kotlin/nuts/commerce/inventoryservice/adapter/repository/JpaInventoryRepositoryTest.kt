package nuts.commerce.inventoryservice.adapter.repository

import nuts.commerce.inventoryservice.model.Inventory
import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName
import java.util.UUID

@Suppress("NonAsciiCharacters")
@DataJpaTest
@Import(JpaInventoryRepository::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaInventoryRepositoryTest {

    @Autowired
    lateinit var repository: InventoryRepository

    @Autowired
    lateinit var inventoryJpa: InventoryJpa

    @BeforeEach
    fun clear() {
        inventoryJpa.deleteAll()
    }

    companion object {
        @ServiceConnection
        @Container
        val db = PostgreSQLContainer(DockerImageName.parse("postgres:15.3-alpine"))
    }

    @Test
    fun `새로운 Inventory를 저장할 수 있다`() {
        // given
        val inventoryId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val productName = "테스트 상품"
        val availableQuantity = 100L

        val inventory = Inventory.create(
            inventoryId = inventoryId,
            idempotencyKey = idempotencyKey,
            productId = productId,
            productName = productName,
            availableQuantity = availableQuantity
        )

        // when
        val savedId = repository.save(inventory)

        // then
        assertEquals(inventoryId, savedId)
        val inventories = repository.getAllCurrentInventory()
        assertEquals(1, inventories.size)
        assertEquals(productId, inventories[0].productId)
        assertEquals(productName, inventories[0].productName)
        assertEquals(availableQuantity, inventories[0].availableQuantity)
    }

    @Test
    fun `동일한 productId와 idempotencyKey로 저장하려고 하면 실패한다`() {
        // given
        val productId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()

        val inventory1 = Inventory.create(
            inventoryId = UUID.randomUUID(),
            idempotencyKey = idempotencyKey,
            productId = productId,
            productName = "상품1",
            availableQuantity = 100L
        )

        val inventory2 = Inventory.create(
            inventoryId = UUID.randomUUID(),
            idempotencyKey = idempotencyKey,
            productId = productId,
            productName = "상품2",
            availableQuantity = 200L
        )

        // when
        repository.save(inventory1)

        // then - 유니크 제약 조건 위반
        assertThrows<DataIntegrityViolationException> {
            repository.save(inventory2)
        }
    }

    @Test
    fun `서로 다른 idempotencyKey는 같은 productId로도 여러 개 저장할 수 있다`() {
        // given
        val productId = UUID.randomUUID()
        val idempotencyKey1 = UUID.randomUUID()
        val idempotencyKey2 = UUID.randomUUID()

        val inventory1 = Inventory.create(
            inventoryId = UUID.randomUUID(),
            idempotencyKey = idempotencyKey1,
            productId = productId,
            productName = "상품",
            availableQuantity = 100L
        )

        val inventory2 = Inventory.create(
            inventoryId = UUID.randomUUID(),
            idempotencyKey = idempotencyKey2,
            productId = productId,
            productName = "상품",
            availableQuantity = 200L
        )

        // when
        val id1 = repository.save(inventory1)
        val id2 = repository.save(inventory2)

        // then
        assertNotEquals(id1, id2)
        val inventories = repository.getAllCurrentInventory()
        assertEquals(2, inventories.size)
    }

    @Test
    fun `인벤토리를 예약할 수 있다`() {
        // given
        val productId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val availableQuantity = 100L
        val reserveQuantity = 30L

        val inventory = Inventory.create(
            idempotencyKey = idempotencyKey,
            productId = productId,
            productName = "테스트 상품",
            availableQuantity = availableQuantity
        )
        repository.save(inventory)

        // when
        val result = repository.reserveInventory(productId, reserveQuantity)

        // then
        assertTrue(result)
        val inventories = repository.getAllCurrentInventory()
        assertEquals(1, inventories.size)
        assertEquals(availableQuantity - reserveQuantity, inventories[0].availableQuantity)
    }

    @Test
    fun `재고가 부족하면 예약에 실패한다`() {
        // given
        val productId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val availableQuantity = 50L
        val reserveQuantity = 100L

        val inventory = Inventory.create(
            idempotencyKey = idempotencyKey,
            productId = productId,
            productName = "테스트 상품",
            availableQuantity = availableQuantity
        )
        repository.save(inventory)

        // when
        val result = repository.reserveInventory(productId, reserveQuantity)

        // then
        assertFalse(result)
        val inventories = repository.getAllCurrentInventory()
        assertEquals(availableQuantity, inventories[0].availableQuantity)
    }

    @Test
    fun `예약된 인벤토리를 확정할 수 있다`() {
        // given
        val productId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val availableQuantity = 100L
        val reserveQuantity = 30L

        val inventory = Inventory.create(
            idempotencyKey = idempotencyKey,
            productId = productId,
            productName = "테스트 상품",
            availableQuantity = availableQuantity,
            reservationQuantity = reserveQuantity
        )
        repository.save(inventory)

        // when
        val result = repository.confirmReservedInventory(productId, reserveQuantity)

        // then
        assertTrue(result)
    }

    @Test
    fun `예약된 인벤토리를 해제할 수 있다`() {
        // given
        val productId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val availableQuantity = 70L
        val reservedQuantity = 30L

        val inventory = Inventory.create(
            idempotencyKey = idempotencyKey,
            productId = productId,
            productName = "테스트 상품",
            availableQuantity = availableQuantity,
            reservationQuantity = reservedQuantity
        )
        repository.save(inventory)

        // when
        val result = repository.releaseReservedInventory(productId, reservedQuantity)

        // then
        assertTrue(result)
        val inventories = repository.getAllCurrentInventory()
        assertEquals(availableQuantity + reservedQuantity, inventories[0].availableQuantity)
    }


    @Test
    fun `전체 인벤토리 정보를 조회할 수 있다`() {
        // given
        val product1Id = UUID.randomUUID()
        val product2Id = UUID.randomUUID()

        val inventory1 = Inventory.create(
            idempotencyKey = UUID.randomUUID(),
            productId = product1Id,
            productName = "상품1",
            availableQuantity = 100L
        )

        val inventory2 = Inventory.create(
            idempotencyKey = UUID.randomUUID(),
            productId = product2Id,
            productName = "상품2",
            availableQuantity = 200L
        )

        repository.save(inventory1)
        repository.save(inventory2)

        // when
        val inventories = repository.getAllCurrentInventory()

        // then
        assertEquals(2, inventories.size)
        assertTrue(inventories.any { it.productId == product1Id && it.availableQuantity == 100L })
        assertTrue(inventories.any { it.productId == product2Id && it.availableQuantity == 200L })
    }

}