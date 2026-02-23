package nuts.commerce.productservice.adapter.repository

import nuts.commerce.productservice.model.Money
import nuts.commerce.productservice.model.Product
import nuts.commerce.productservice.port.repository.ProductRepository
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("NonAsciiCharacters")
@DataJpaTest
@Import(JpaProductRepository::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaProductRepositoryTest {

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var productJpa: ProductJpa

    companion object {
        @ServiceConnection
        @Container
        val postgres = PostgreSQLContainer("postgres:15.3-alpine")
    }

    @BeforeEach
    fun clear() {
        productJpa.deleteAll()
    }

    @Test
    fun `save 후 getProduct로 조회된다`() {
        val productId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()

        val savedId = productRepository.save(
            Product.create(
                productId = productId,
                productName = "keyboard",
                idempotencyKey = idempotencyKey,
                price = Money(12000L, "KRW")
            )
        )

        val found = productRepository.getProduct(savedId)

        assertNotNull(found)
        assertEquals(productId, found.productId)
        assertEquals("keyboard", found.productName)
        assertEquals(12000L, found.price.amount)
        assertEquals("KRW", found.price.currency)
    }

    @Test
    fun `getAllProductInfo는 저장된 목록과 필드를 반환한다`() {
        val p1 = Product.create(
            productId = UUID.randomUUID(),
            productName = "mouse",
            idempotencyKey = UUID.randomUUID(),
            price = Money(5000L, "KRW")
        )
        val p2 = Product.create(
            productId = UUID.randomUUID(),
            productName = "pad",
            idempotencyKey = UUID.randomUUID(),
            price = Money(2000L, "KRW")
        )

        productRepository.save(p1)
        productRepository.save(p2)

        val all = productRepository.getAllProductInfo()

        assertEquals(2, all.size)
        assertTrue(all.any { it.productId == p1.productId && it.productName == "mouse" && it.price.amount == 5000L })
        assertTrue(all.any { it.productId == p2.productId && it.productName == "pad" && it.price.amount == 2000L })
    }

    @Test
    fun `같은 productName 과 같은 idempotencyKey 로 재시도하면 유니크 제약으로 실패한다`() {
        val idempotencyKey = UUID.randomUUID()
        val productName = "dup-name"

        productRepository.save(
            Product.create(
                productName = productName,
                idempotencyKey = idempotencyKey,
                price = Money(1000L, "KRW")
            )
        )

        assertThrows(DataIntegrityViolationException::class.java) {
            productRepository.save(
                Product.create(
                    productName = productName,
                    idempotencyKey = idempotencyKey,
                    price = Money(1200L, "KRW")
                )
            )
        }
    }

    @Test
    fun `존재하지 않는 productId는 null을 반환한다`() {
        val missing = productRepository.getProduct(UUID.randomUUID())
        assertNull(missing)
    }
}