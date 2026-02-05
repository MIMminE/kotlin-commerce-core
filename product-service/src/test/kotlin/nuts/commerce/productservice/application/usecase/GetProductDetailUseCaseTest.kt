package nuts.commerce.productservice.application.usecase

import nuts.commerce.productservice.application.port.repository.InMemoryProductRepository
import nuts.commerce.productservice.application.port.repository.InMemoryStockQuery
import nuts.commerce.productservice.model.domain.Money
import nuts.commerce.productservice.model.domain.Product
import nuts.commerce.productservice.model.exception.ProductException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetProductDetailUseCaseTest {

    private val productRepository = InMemoryProductRepository()
    private val stockQuery = InMemoryStockQuery()
    private val useCase = GetProductDetailUseCase(productRepository, stockQuery)

    @BeforeEach
    fun setup() {
        productRepository.clear()
        stockQuery.clear()
    }

    @Test
    fun `정상 케이스 - 상세 정보 반환`() {
        val product = Product.create(productName = "p-1", price = Money(1500L, "KRW"))
        productRepository.save(product)
        stockQuery.setStock(product.productId, 7)

        val detail = useCase.execute(product.productId) ?: kotlin.test.fail("product detail is null")

        assertEquals(product.productId, detail.productId)
        assertEquals(product.productName, detail.productName)
        assertEquals(7, detail.stock)
        assertEquals(1500L, detail.price)
        assertEquals("KRW", detail.currency)
    }

    @Test
    fun `제품이 없으면 ProductException InvalidCommand 예외`() {
        val missing = UUID.randomUUID()
        assertFailsWith<ProductException.InvalidCommand> {
            useCase.execute(missing)
        }
    }
}
