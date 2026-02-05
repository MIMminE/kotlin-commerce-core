package nuts.commerce.inventoryservice.application.usecase

import nuts.commerce.inventoryservice.application.port.message.InMemoryInventoryCachePublisher
import nuts.commerce.inventoryservice.application.port.repository.InMemoryInventoryRepository
import nuts.commerce.inventoryservice.model.domain.Inventory
import nuts.commerce.inventoryservice.model.domain.Inventory.InventoryStatus
import nuts.commerce.inventoryservice.model.InventoryException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class InventoryUseCaseTest {

    private lateinit var repo: InMemoryInventoryRepository
    private lateinit var publisher: InMemoryInventoryCachePublisher

    private lateinit var increaseUseCase: IncreaseInventoryUseCase
    private lateinit var decreaseUseCase: DecreaseInventoryUseCase
    private lateinit var markAvailableUseCase: MarkAvailableUseCase
    private lateinit var markUnavailableUseCase: MarkUnavailableUseCase
    private lateinit var deleteUseCase: DeleteInventoryUseCase
    private lateinit var publishUseCase: PublishInventoryChangeUseCase

    @BeforeEach
    fun setup() {
        repo = InMemoryInventoryRepository()
        publisher = InMemoryInventoryCachePublisher()
        increaseUseCase = IncreaseInventoryUseCase(repo, publisher)
        decreaseUseCase = DecreaseInventoryUseCase(repo, publisher)
        markAvailableUseCase = MarkAvailableUseCase(repo, publisher)
        markUnavailableUseCase = MarkUnavailableUseCase(repo, publisher)
        deleteUseCase = DeleteInventoryUseCase(repo, publisher)
        publishUseCase = PublishInventoryChangeUseCase(publisher)
        repo.clear()
        publisher.clear()
    }

    @Test
    fun `증가하면 수량이 늘고 퍼블리시된다`() {
        val inv = Inventory.create(productId = UUID.randomUUID(), quantity = 10)
        repo.save(inv)

        val res = increaseUseCase.execute(inv.inventoryId, 5)

        val saved = repo.findById(inv.inventoryId)!!
        assertEquals(15, saved.quantity)
        assertEquals(15, res.quantity)
        val last = publisher.lastOrNull()
        assertNotNull(last)
        assertEquals(saved.inventoryId, last.inventoryId)
        assertEquals(saved.quantity, last.quantity)
    }

    @Test
    fun `감소하면 수량이 줄고 퍼블리시된다`() {
        val inv = Inventory.create(productId = UUID.randomUUID(), quantity = 10)
        repo.save(inv)

        val res = decreaseUseCase.execute(inv.inventoryId, 4)

        val saved = repo.findById(inv.inventoryId)!!
        assertEquals(6, saved.quantity)
        assertEquals(6, res.quantity)
        val last = publisher.lastOrNull()
        assertNotNull(last)
        assertEquals(saved.inventoryId, last.inventoryId)
        assertEquals(saved.quantity, last.quantity)
    }

    @Test
    fun `수량부족이면 예외를 던진다`() {
        val inv = Inventory.create(productId = UUID.randomUUID(), quantity = 2)
        repo.save(inv)

        assertFailsWith<InventoryException.InsufficientInventory> {
            decreaseUseCase.execute(inv.inventoryId, 5)
        }
    }

    @Test
    fun `가용화 후 비가용화 전이가 발생하고 퍼블리시된다`() {
        val inv = Inventory.create(productId = UUID.randomUUID(), quantity = 1, status = InventoryStatus.UNAVAILABLE)
        repo.save(inv)

        val res = markAvailableUseCase.execute(inv.inventoryId)
        val saved = repo.findById(inv.inventoryId)!!
        assertEquals(InventoryStatus.AVAILABLE, saved.status)
        assertEquals(saved.inventoryId, res.inventoryId)
        val last = publisher.lastOrNull()
        assertNotNull(last)
        assertEquals(saved.inventoryId, last.inventoryId)

        val res2 = markUnavailableUseCase.execute(inv.inventoryId)
        val saved2 = repo.findById(inv.inventoryId)!!
        assertEquals(InventoryStatus.UNAVAILABLE, saved2.status)
        assertEquals(saved2.inventoryId, res2.inventoryId)
    }

    @Test
    fun `삭제하면 상태가 DELETED가 되고 퍼블리시된다`() {
        val inv = Inventory.create(productId = UUID.randomUUID(), quantity = 3)
        repo.save(inv)

        val res = deleteUseCase.execute(inv.inventoryId)
        val saved = repo.findById(inv.inventoryId)!!
        assertEquals(InventoryStatus.DELETED, saved.status)
        assertEquals(saved.inventoryId, res.inventoryId)
        assertNotNull(publisher.lastOrNull())
    }

    @Test
    fun `퍼블리시 유스케이스가 퍼블리셔에 위임한다`() {
        val inv = Inventory.create(productId = UUID.randomUUID(), quantity = 7)
        repo.save(inv)

        publishUseCase.execute(inv.inventoryId, inv.productId, inv.quantity)
        val last = publisher.lastOrNull()
        assertNotNull(last)
        assertEquals(inv.inventoryId, last.inventoryId)
        assertEquals(7, last.quantity)
    }
}
