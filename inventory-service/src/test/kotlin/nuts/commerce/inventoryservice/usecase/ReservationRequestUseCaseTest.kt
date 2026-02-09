package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.adapter.repository.JpaInventoryRepository
import nuts.commerce.inventoryservice.adapter.repository.JpaReservationRepository
import nuts.commerce.inventoryservice.model.Inventory
import nuts.commerce.inventoryservice.model.InventoryStatus
import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.DefaultTransactionDefinition
import java.util.UUID

@SpringBootTest
class ReservationRequestUseCaseTest @Autowired constructor(
    private val reservationRequestUseCase: ReservationRequestUseCase,
    private val inventoryRepository: InventoryRepository,
    private val reservationRepository: JpaReservationRepository,
    private val txManager: PlatformTransactionManager
) {

    @Test
    fun `멱등성 - 같은 idempotencyKey 재요청하면 기존 예약 반환`() {
        val orderId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val productId = UUID.randomUUID()

        // 인벤토리 준비
        val inv = Inventory.create(idempotencyKey = UUID.randomUUID(), productId = productId, availableQuantity = 100L)
        inventoryRepository.save(inv)

        val items = listOf(ReservationRequestCommand.Item(productId = productId, qty = 2L))
        val cmd = ReservationRequestCommand(orderId = orderId, idempotencyKey = idempotencyKey, items = items)

        val r1 = reservationRequestUseCase.execute(cmd)
        val r2 = reservationRequestUseCase.execute(cmd)

        assertEquals(r1.reservationId, r2.reservationId)
    }

    @Test
    fun `동시성 - 두 트랜잭션에서 동일 인벤토리 수정시 낙관적 락 발생`() {
        val productId = UUID.randomUUID()
        val inv = Inventory.create(idempotencyKey = UUID.randomUUID(), productId = productId, availableQuantity = 100L)
        val saved = inventoryRepository.save(inv)

        // 트랜잭션 A 시작
        val defA = DefaultTransactionDefinition()
        defA.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRED
        val txA = txManager.getTransaction(defA)
        val invA = inventoryRepository.findAllByProductIdIn(listOf(productId)).first()
        invA.reserve(10)
        inventoryRepository.save(invA)

        // 트랜잭션 B (새 트랜잭션)에서 동일 엔티티 수정 및 커밋
        val defB = DefaultTransactionDefinition()
        defB.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        val txB = txManager.getTransaction(defB)
        val invB = inventoryRepository.findAllByProductIdIn(listOf(productId)).first()
        invB.reserve(20)
        inventoryRepository.save(invB)
        txManager.commit(txB)

        // 트랜잭션 A 커밋 시 낙관적 락 예외 발생
        assertThrows(ObjectOptimisticLockingFailureException::class.java) {
            txManager.commit(txA)
        }
    }
}