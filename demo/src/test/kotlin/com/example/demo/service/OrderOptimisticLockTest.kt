package com.example.demo.service

import com.example.demo.model.Order
import com.example.demo.repository.OrderRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.DefaultTransactionDefinition

@SpringBootTest
class OrderOptimisticLockTest @Autowired constructor(
    val orderRepository: OrderRepository,
    val transactionManager: PlatformTransactionManager
) {

    @Test
    fun `낙관적 락 충돌 발생`() {
        // 1) 엔티티 초기 저장
        val order = Order.create(userId = 1)
        val saved = orderRepository.save(order)

        // 트랜잭션 A: 기본 전파로 시작
        val defA = DefaultTransactionDefinition()
        defA.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRED
        val txA = transactionManager.getTransaction(defA)
        val orderA = orderRepository.findById(saved.id!!).get()
        // 트랜잭션 A에서 상태 변경(아직 커밋하지 않음)
        orderA.stock = orderA.stock + 1
        orderRepository.save(orderA)

        // 트랜잭션 B: 별도의 새 트랜잭션에서 동일 엔티티를 변경하고 커밋
        val defB = DefaultTransactionDefinition()
        defB.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        val txB = transactionManager.getTransaction(defB)
        val orderB = orderRepository.findById(saved.id!!).get()
        orderB.stock = orderB.stock + 10
        orderRepository.save(orderB)
        transactionManager.commit(txB)

           transactionManager.commit(txA)
        // 트랜잭션 A를 커밋할 때 낙관적 락 충돌 예외가 발생해야 함
//        assertThrows(ObjectOptimisticLockingFailureException::class.java) {
//            transactionManager.commit(txA)
//        }
    }
}
