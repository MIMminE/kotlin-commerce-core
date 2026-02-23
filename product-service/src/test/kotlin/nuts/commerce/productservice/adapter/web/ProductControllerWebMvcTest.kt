package nuts.commerce.productservice.adapter.web

import nuts.commerce.productservice.usecase.GetProductDetailUseCase
import nuts.commerce.productservice.usecase.GetProductsUseCase
import nuts.commerce.productservice.usecase.RegisterProductUseCase
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.Test
import nuts.commerce.productservice.model.Money
import nuts.commerce.productservice.usecase.ProductSummary
import org.hamcrest.Matchers.hasSize


@Suppress("NonAsciiCharacters")
@WebMvcTest(ProductController::class)
class ProductControllerWebMvcTest(
) {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @field:MockitoBean
    lateinit var registerProductUseCase: RegisterProductUseCase

    @field:MockitoBean
    lateinit var getProductsUseCase: GetProductsUseCase

    @field:MockitoBean
    lateinit var getProductDetailUseCase: GetProductDetailUseCase

    @Test
    fun `POST registerProduct - 성공`() {
        val idempotency = UUID.randomUUID()

        val req = RegisterRequest("name", 1000L, "KRW")
        val result = RegisterProductUseCase.RegisteredProduct(UUID.randomUUID(), "name")

        whenever(registerProductUseCase.execute(any()))
            .thenReturn(result)

        mockMvc.perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotency.toString())
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.productName").value("name"))

        verify(registerProductUseCase).execute(any())
    }

    @Test
    fun `GET getProducts - 성공`() {
        val product1Id = UUID.randomUUID()
        val product2Id = UUID.randomUUID()

        val products = listOf(
            ProductSummary(
                productId = product1Id,
                productName = "상품1",
                price = Money(10000L, "KRW"),
                stock = 100L
            ),
            ProductSummary(
                productId = product2Id,
                productName = "상품2",
                price = Money(20000L, "KRW"),
                stock = 50L
            )
        )

        whenever(getProductsUseCase.execute())
            .thenReturn(products)

        mockMvc.perform(
            get("/api/products/search/all")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.products", hasSize<Any>(2)))
            .andExpect(jsonPath("$.products[0].productName").value("상품1"))
            .andExpect(jsonPath("$.products[0].stock").value(100))
            .andExpect(jsonPath("$.products[1].productName").value("상품2"))
            .andExpect(jsonPath("$.products[1].stock").value(50))

        verify(getProductsUseCase).execute()
    }

    @Test
    fun `GET getProductDetail - 성공`() {
        val productId = UUID.randomUUID()

        val detail = GetProductDetailUseCase.ProductDetail(
            productId = productId,
            productName = "상품상세",
            stock = 30L,
            price = 15000L,
            currency = "KRW"
        )

        whenever(getProductDetailUseCase.execute(productId))
            .thenReturn(detail)

        mockMvc.perform(
            get("/api/products/search/$productId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.productName").value("상품상세"))
            .andExpect(jsonPath("$.stock").value(30))
            .andExpect(jsonPath("$.price").value(15000))
            .andExpect(jsonPath("$.currency").value("KRW"))

        verify(getProductDetailUseCase).execute(productId)
    }
}
