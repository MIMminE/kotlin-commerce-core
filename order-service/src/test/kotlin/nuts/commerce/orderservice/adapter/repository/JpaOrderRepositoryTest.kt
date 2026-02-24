package nuts.commerce.orderservice.adapter.repository

import nuts.commerce.orderservice.model.Money
import nuts.commerce.orderservice.model.Order
import nuts.commerce.orderservice.model.OrderItem
import nuts.commerce.orderservice.model.OrderStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName
import java.util.*

@Suppress("NonAsciiCharacters")
@DataJpaTest
@Import(JpaOrderRepository::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaOrderRepositoryTest {

    @Autowired
    private lateinit var repository: JpaOrderRepository

    companion object {
        @ServiceConnection
        @Container
        val db = PostgreSQLContainer(DockerImageName.parse("postgres:15.3-alpine"))
    }

    @Test
    fun `Order를 저장하고 조회할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val userId = "user123"
        val idempotencyKey = UUID.randomUUID()

        val order = Order.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey,
            userId = userId
        )
        val items = listOf(
            OrderItem.create(
                productId = UUID.randomUUID(),
                qty = 1,
                unitPrice = Money(10000, "KRW")
            )
        )
        order.addItems(items)

        // when
        val saved = repository.save(order)

        // then
        assertNotNull(saved)
        assertEquals(orderId, saved.orderId)
        assertEquals(userId, saved.userId)
        assertEquals(idempotencyKey, saved.idempotencyKey)
        assertEquals(OrderStatus.CREATED, saved.status)
    }

    @Test
    fun `userId와 idempotencyKey로 Order를 조회할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val userId = "user456"
        val idempotencyKey = UUID.randomUUID()

        val order = Order.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey,
            userId = userId
        )
        val items = listOf(
            OrderItem.create(
                productId = UUID.randomUUID(),
                qty = 1,
                unitPrice = Money(10000, "KRW")
            )
        )
        order.addItems(items)
        repository.save(order)

        // when
        val found = repository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)

        // then
        assertNotNull(found)
        assertEquals(orderId, found?.orderId)
        assertEquals(userId, found?.userId)
    }

    @Test
    fun `userId로 Order 목록을 페이징하여 조회할 수 있다`() {
        // given
        val userId = "user-page-test"
        repeat(5) {
            val order = Order.create(
                orderId = UUID.randomUUID(),
                idempotencyKey = UUID.randomUUID(),
                userId = userId
            )
            val items = listOf(
                OrderItem.create(
                    productId = UUID.randomUUID(),
                    qty = 1,
                    unitPrice = Money(10000, "KRW")
                )
            )
            order.addItems(items)
            repository.save(order)
        }

        // when
        val page1 = repository.findAllByUserId(userId, PageRequest.of(0, 3))
        val page2 = repository.findAllByUserId(userId, PageRequest.of(1, 3))

        // then
        assertEquals(3, page1.content.size)
        assertEquals(2, page2.content.size)
        assertEquals(5, page1.totalElements)
    }

    @Test
    fun `Order의 상태를 변경할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val order = Order.create(
            orderId = orderId,
            idempotencyKey = UUID.randomUUID(),
            userId = "user-status-test"
        )
        val items = listOf(
            OrderItem.create(
                productId = UUID.randomUUID(),
                qty = 1,
                unitPrice = Money(10000, "KRW")
            )
        )
        order.addItems(items)
        repository.save(order)

        // when
        repository.updateStatus(orderId, OrderStatus.CREATED, OrderStatus.PAYING)

        // then
        val updated = repository.findById(orderId)
        assertNotNull(updated)
        assertEquals(OrderStatus.PAYING, updated?.status)
    }

    @Test
    fun `다른 상태에서는 Order 상태 변경이 실패한다`() {
        // given
        val orderId = UUID.randomUUID()
        val order = Order.create(
            orderId = orderId,
            idempotencyKey = UUID.randomUUID(),
            userId = "user-status-fail-test"
        )
        val items = listOf(
            OrderItem.create(
                productId = UUID.randomUUID(),
                qty = 1,
                unitPrice = Money(10000, "KRW")
            )
        )
        order.addItems(items)
        repository.save(order)

        // when & then - 기대하지 않은 상태에서 변경하려고 하면 예외 발생
        assertThrows<IllegalStateException> {
            repository.updateStatus(orderId, OrderStatus.PAYING, OrderStatus.PAID)
        }
    }


    @Test
    fun `동일한 userId와 idempotencyKey로 두 개의 Order를 저장할 수 없다`() {
        // given
        val userId = "user-unique-test"
        val idempotencyKey = UUID.randomUUID()

        val order1 = Order.create(
            orderId = UUID.randomUUID(),
            idempotencyKey = idempotencyKey,
            userId = userId
        )

        val order2 = Order.create(
            orderId = UUID.randomUUID(),
            idempotencyKey = idempotencyKey,
            userId = userId
        )

        val items = listOf(
            OrderItem.create(
                productId = UUID.randomUUID(),
                qty = 1,
                unitPrice = Money(10000, "KRW")
            )
        )
        order1.addItems(items)
        repository.save(order1)

        val items2 = listOf(
            OrderItem.create(
                productId = UUID.randomUUID(),
                qty = 1,
                unitPrice = Money(10000, "KRW")
            )
        )
        order2.addItems(items2)

        // when & then - 중복 저장 시도는 실패
        assertThrows<Exception> {
            repository.save(order2)
        }
    }


    @Test
    fun `상품이 없는 Order는 저장할 수 없다`() {
        // given
        val orderId = UUID.randomUUID()
        val order = Order.create(
            orderId = orderId,
            idempotencyKey = UUID.randomUUID(),
            userId = "user-no-items-test"
        )
        // 상품을 추가하지 않음

        // when & then - 상품이 없으면 저장 시 예외 발생
        assertThrows<Exception> {
            repository.save(order)
        }
    }
}


