package nuts.commerce.productservice.application.usecase

import nuts.commerce.productservice.application.adapter.repository.InMemoryProductRepository
import nuts.commerce.productservice.model.Money
import nuts.commerce.productservice.model.Product
import nuts.commerce.productservice.model.ProductStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("NonAsciiCharacters")
class DeleteProductUseCaseTest {

    private val productRepository = InMemoryProductRepository()
    private val useCase = DeleteProductUseCase(productRepository)

    @BeforeEach
    fun setup() {
        productRepository.clear()
    }

    @Test
    fun `제품을 삭제하면 상태가 DELETED 로 변경된다`() {
        val p = Product.create(productName = "toDelete", price = Money(100L, "KRW"), status = ProductStatus.ACTIVE)
        productRepository.save(p)

        val res = useCase.execute(p.productId)

        assertNotNull(res.productId)
        assertEquals(ProductStatus.ACTIVE, res.beforeStatus)
        assertEquals(ProductStatus.DELETED, res.afterStatus)

        val saved = productRepository.findById(p.productId)
        assertEquals(ProductStatus.DELETED, saved.status)
    }
}

