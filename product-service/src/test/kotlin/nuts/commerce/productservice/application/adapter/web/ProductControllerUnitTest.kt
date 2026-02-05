package nuts.commerce.productservice.application.adapter.web

import nuts.commerce.productservice.application.port.repository.InMemoryProductRepository
import nuts.commerce.productservice.application.port.repository.InMemoryStockQuery
import nuts.commerce.productservice.application.usecase.*
import nuts.commerce.productservice.model.domain.Money
import nuts.commerce.productservice.model.domain.Product
import nuts.commerce.productservice.model.domain.ProductStatus
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.ObjectMapper

class ProductControllerUnitTest {

    private lateinit var repo: InMemoryProductRepository
    private lateinit var stockQuery: InMemoryStockQuery
    private lateinit var controller: ProductController
    private lateinit var mockMvc: MockMvc
    private val mapper = ObjectMapper()

    @BeforeEach
    fun setup() {
        repo = InMemoryProductRepository()
        stockQuery = InMemoryStockQuery()
        val regUseCase = RegisterProductUseCase(repo)
        val getUseCase = GetProductsUseCase(repo)
        val detailUseCase = GetProductDetailUseCase(repo, stockQuery)
        val activateUseCase = ActivateProductUseCase(repo)
        val deactivateUseCase = DeactivateProductUseCase(repo)
        val deleteUseCase = DeleteProductUseCase(repo)
        controller = ProductController(regUseCase, getUseCase, detailUseCase, activateUseCase, deactivateUseCase, deleteUseCase)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        repo.clear()
        stockQuery.clear()
    }

    @Test
    fun `상품 등록 시 201 및 저장된 항목을 반환한다`() {
        val req = mapOf("productName" to "new", "price" to 2000L, "currency" to "KRW")

        mockMvc.perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.productName").value("new"))
            .andExpect(jsonPath("$.productId").value(notNullValue()))
    }

    @Test
    fun `활성 제품만 조회된다`() {
        val p1 = Product.create(productName = "p1", price = Money(100L, "KRW"), status = ProductStatus.ACTIVE)
        val p2 = Product.create(productName = "p2", price = Money(200L, "KRW"), status = ProductStatus.INACTIVE)
        val p3 = Product.create(productName = "p3", price = Money(300L, "KRW"), status = ProductStatus.ACTIVE)

        repo.save(p1)
        repo.save(p2)
        repo.save(p3)

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()", `is`(2)))
            .andExpect(jsonPath("$[?(@.productName == 'p1')]").exists())
    }

    @Test
    fun `상세 조회 시 재고 포함 상세 정보를 반환한다`() {
        val p = Product.create(productName = "detail", price = Money(1500L, "KRW"))
        repo.save(p)
        stockQuery.setStock(p.productId, 5)

        mockMvc.perform(get("/api/products/${p.productId}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.productId").value(p.productId.toString()))
            .andExpect(jsonPath("$.productName").value("detail"))
            .andExpect(jsonPath("$.stock").value(5))
            .andExpect(jsonPath("$.price").value(1500))
    }

    @Test
    fun `제품 활성화 엔드포인트가 작동한다`() {
        val p = Product.create(productName = "toActivate", price = Money(100L, "KRW"), status = ProductStatus.INACTIVE)
        repo.save(p)

        mockMvc.perform(post("/api/products/${p.productId}/activate"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.productId").value(p.productId.toString()))

        val saved = repo.findById(p.productId) ?: throw AssertionError("product missing")
        assert(saved.status == ProductStatus.ACTIVE)
    }

    @Test
    fun `제품 비활성화 엔드포인트가 작동한다`() {
        val p = Product.create(productName = "toDeactivate", price = Money(100L, "KRW"), status = ProductStatus.ACTIVE)
        repo.save(p)

        mockMvc.perform(post("/api/products/${p.productId}/deactivate"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.productId").value(p.productId.toString()))

        val saved = repo.findById(p.productId) ?: throw AssertionError("product missing")
        assert(saved.status == ProductStatus.INACTIVE)
    }

    @Test
    fun `제품 삭제 엔드포인트가 작동한다`() {
        val p = Product.create(productName = "toDelete", price = Money(100L, "KRW"), status = ProductStatus.ACTIVE)
        repo.save(p)

        mockMvc.perform(post("/api/products/${p.productId}/delete"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.productId").value(p.productId.toString()))

        val saved = repo.findById(p.productId) ?: throw AssertionError("product missing")
        assert(saved.status == ProductStatus.DELETED)
    }
}