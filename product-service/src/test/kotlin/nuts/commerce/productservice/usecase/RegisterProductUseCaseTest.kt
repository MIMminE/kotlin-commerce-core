package nuts.commerce.productservice.usecase

import nuts.commerce.productservice.testutil.InMemoryProductRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

@Suppress("NonAsciiCharacters")
class RegisterProductUseCaseTest {
    private lateinit var repo: InMemoryProductRepository
    private lateinit var useCase: RegisterProductUseCase

    @BeforeEach
    fun setup() {
        repo = InMemoryProductRepository()
        useCase = RegisterProductUseCase(repo)
    }

    @Test
    fun `성공 - 상품 등록 후 아이디 반환`() {
        val cmd = RegisterProductCommand(UUID.randomUUID(), "p1", 1000L, "KRW")

        val result = useCase.execute(cmd)

        val saved = repo.getProduct(result.productId)
        assertEquals("p1", result.productName)
        assertEquals(result.productId, saved?.productId)
    }
}

