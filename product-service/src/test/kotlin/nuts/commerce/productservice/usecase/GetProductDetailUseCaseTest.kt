package nuts.commerce.productservice.usecase

import nuts.commerce.productservice.testutil.InMemoryStockCache
import nuts.commerce.productservice.testutil.InMemoryProductRepository
import nuts.commerce.productservice.model.Money
import nuts.commerce.productservice.model.Product
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

@Suppress("NonAsciiCharacters")
class GetProductDetailUseCaseTest {
    private lateinit var repo: InMemoryProductRepository
    private lateinit var stockCache: InMemoryStockCache
    private lateinit var useCase: GetProductDetailUseCase

    @BeforeEach
    fun setup() {
        repo = InMemoryProductRepository()
        stockCache = InMemoryStockCache()
        useCase = GetProductDetailUseCase(repo, stockCache)
        repo.clear()
        stockCache.clear()
    }

    @Test
    fun `성공 - 상세 조회`() {
        val p = Product.create(productName = "detail", price = Money(1500L, "KRW"), idempotencyKey = UUID.randomUUID())
        repo.save(p)
        stockCache.saveStock(p.productId, 5)

        val result = useCase.execute(p.productId)

        assertEquals(p.productId, result.productId)
        assertEquals(5, result.stock)
        assertEquals(1500L, result.price)
    }

    @Test
    fun `실패 - 존재하지 않는 상품이면 IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase.execute(UUID.randomUUID())
        }
    }
}

