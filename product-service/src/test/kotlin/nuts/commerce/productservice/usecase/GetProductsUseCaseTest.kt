package nuts.commerce.productservice.usecase

import nuts.commerce.productservice.testutil.InMemoryStockCache
import nuts.commerce.productservice.testutil.InMemoryProductRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import nuts.commerce.productservice.model.Product
import nuts.commerce.productservice.model.Money
import java.util.UUID

@Suppress("NonAsciiCharacters")
class GetProductsUseCaseTest {
    private lateinit var repo: InMemoryProductRepository
    private lateinit var stockCache: InMemoryStockCache
    private lateinit var useCase: GetProductsUseCase

    @BeforeEach
    fun setup() {
        repo = InMemoryProductRepository()
        stockCache = InMemoryStockCache()
        useCase = GetProductsUseCase(repo, stockCache)
        repo.clear()
        stockCache.clear()
    }

    @Test
    fun `성공 - 모든 상품 요약 반환`() {
        val p1 = Product.create(productName = "p1", price = Money(100L, "KRW"), idempotencyKey = UUID.randomUUID())
        val p2 = Product.create(productName = "p2", price = Money(200L, "KRW"), idempotencyKey = UUID.randomUUID())
        repo.save(p1)
        repo.save(p2)

        stockCache.saveStock(p1.productId, 5)
        stockCache.saveStock(p2.productId, 3)

        val result = useCase.execute()

        assertEquals(2, result.size)
    }

    @Test
    fun `실패 - 상품 정보가 비어있으면 IllegalStateException`() {
        repo.clear()
        assertThrows(IllegalStateException::class.java) {
            useCase.execute()
        }
    }

    @Test
    fun `실패 - 재고 정보 누락 시 IllegalStateException`() {
        val p1 = Product.create(productName = "p1", price = Money(100L, "KRW"), idempotencyKey = UUID.randomUUID())
        repo.save(p1)
        // 재고 미저장
        assertThrows(IllegalStateException::class.java) {
            useCase.execute()
        }
    }
}

