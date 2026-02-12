package nuts.commerce.productservice.application.usecase

import nuts.commerce.productservice.application.adapter.repository.InMemoryProductRepository
import nuts.commerce.productservice.usecase.RegisterProductUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("NonAsciiCharacters")
class RegisterProductUseCaseTest {

    private val productRepository = InMemoryProductRepository()
    private val useCase = RegisterProductUseCase(productRepository)

    @BeforeEach
    fun setup() {
        productRepository.clear()
    }

    @Test
    fun `정상 등록 - 등록된 Product 정보 반환`() {
        val cmd = RegisterProductUseCase.RegisterProductCommand(productName = "new", price = 2000L, currency = "KRW")

        val res = useCase.execute(cmd)

        assertNotNull(res.productId)
        assertEquals("new", res.productName)

        // 저장소에서 조회 가능
        val saved = productRepository.getProduct(res.productId)
        assertEquals("new", saved.productName)
    }
}
