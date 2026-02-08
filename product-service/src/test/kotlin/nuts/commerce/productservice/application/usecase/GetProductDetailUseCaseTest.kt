package nuts.commerce.productservice.application.usecase

import nuts.commerce.productservice.application.adapter.repository.InMemoryProductRepository
import nuts.commerce.productservice.model.Money
import nuts.commerce.productservice.model.Product
import nuts.commerce.productservice.exception.ProductException
import nuts.commerce.productservice.application.adapter.cache.InMemoryProductStockCachePort
import nuts.commerce.productservice.usecase.GetProductDetailUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Suppress("NonAsciiCharacters")
class GetProductDetailUseCaseTest {

    private val productRepository = InMemoryProductRepository()
    private val productStockCache = InMemoryProductStockCachePort()
    private val useCase = GetProductDetailUseCase(productRepository, productStockCache)

    @BeforeEach
    fun setup() {
        productRepository.clear()
        productStockCache.clear()
    }

    @Test
    fun `정상 케이스 - 상세 정보 반환`() {
        val product = Product.create(productName = "p-1", price = Money(1500L, "KRW"))
        productRepository.save(product)
        productStockCache.setStock(product.productId, 7)

        val detail = useCase.execute(product.productId)

        assertEquals(product.productId, detail.productId)
        assertEquals(product.productName, detail.productName)
        assertEquals(7, detail.stock)
        assertEquals(1500L, detail.price)
        assertEquals("KRW", detail.currency)
    }

    @Test
    fun `상품에 대한 재고 수량이 없으면 ProductException InvalidCommand 예외`() {
        val missing = UUID.randomUUID()
        assertFailsWith<ProductException.InvalidCommand> {
            useCase.execute(missing)
        }
    }
}
