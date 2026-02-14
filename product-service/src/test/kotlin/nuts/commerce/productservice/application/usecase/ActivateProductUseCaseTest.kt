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
class ActivateProductUseCaseTest {

    private val productRepository = InMemoryProductRepository()
    private val useCase = ActivateProductUseCase(productRepository)

    @BeforeEach
    fun setup() {
        productRepository.clear()
    }

    @Test
    fun `제품을 활성화하면 상태가 ACTIVE 로 변경된다`() {
        val p = Product.create(productName = "toActivate", price = Money(100L, "KRW"), status = ProductStatus.INACTIVE)
        productRepository.save(p)

        val res = useCase.execute(p.productId)

        assertNotNull(res.productId)
        assertEquals(ProductStatus.INACTIVE, res.beforeStatus)
        assertEquals(ProductStatus.ACTIVE, res.afterStatus)

        val saved = productRepository.findById(p.productId)
        assertEquals(ProductStatus.ACTIVE, saved.status)
    }
}

