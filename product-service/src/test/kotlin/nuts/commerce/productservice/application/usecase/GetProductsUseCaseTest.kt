package nuts.commerce.productservice.application.usecase

import nuts.commerce.productservice.application.adapter.repository.InMemoryProductRepository
import nuts.commerce.productservice.model.Money
import nuts.commerce.productservice.model.Product
import nuts.commerce.productservice.model.ProductStatus
import nuts.commerce.productservice.usecase.GetProductsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@Suppress("NonAsciiCharacters")
class GetProductsUseCaseTest {

    private val productRepository = InMemoryProductRepository()
    private val useCase = GetProductsUseCase(productRepository)

    @BeforeEach
    fun setup() {
        productRepository.clear()
    }

    @Test
    fun `정상 조회 - 활성화된 제품들 반환`() {
        val p1 = Product.create(productName = "p1", price = Money(100L, "KRW"), status = ProductStatus.ACTIVE)
        val p2 = Product.create(productName = "p2", price = Money(200L, "KRW"), status = ProductStatus.INACTIVE)
        val p3 = Product.create(productName = "p3", price = Money(300L, "KRW"), status = ProductStatus.ACTIVE)

        productRepository.save(p1)
        productRepository.save(p2)
        productRepository.save(p3)

        val list = useCase.execute()

        assertEquals(2, list.size)
        val names = list.map { it.productName }
        assertEquals(true, names.containsAll(listOf("p1", "p3")))
    }
}
