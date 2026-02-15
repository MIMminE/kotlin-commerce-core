package nuts.commerce.productservice.adapter.web

import nuts.commerce.productservice.usecase.GetProductDetailUseCase
import nuts.commerce.productservice.usecase.GetProductsUseCase
import nuts.commerce.productservice.usecase.RegisterProductCommand
import nuts.commerce.productservice.usecase.RegisterProductUseCase
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val registerProductUseCase: RegisterProductUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val getProductDetailUseCase: GetProductDetailUseCase,
) {

    @PostMapping
    fun registerProduct(
        @RequestHeader("Idempotency-Key") idempotencyKey: UUID,
        @RequestBody request: RegisterRequest
    ): RegisterResponse {
        val command = RegisterProductCommand(
            idempotencyKey = idempotencyKey,
            productName = request.productName,
            price = request.price,
            currency = request.currency
        )
        val result = registerProductUseCase.execute(command)
        return RegisterResponse(
            productId = result.productId,
            productName = result.productName
        )
    }

    @GetMapping("/search/all")
    fun getProducts(): List<ProductSummaryResponse> {
        val products = getProductsUseCase.execute()
        return products.map {
            ProductSummaryResponse(
                productId = it.productId,
                productName = it.productName.toString()
            )
        }
    }

    @GetMapping("/search/{productId}")
    fun getProductDetail(@PathVariable productId: UUID): ProductDetailResponse {
        val detail = getProductDetailUseCase.execute(productId)
        return ProductDetailResponse(
            productId = detail.productId,
            productName = detail.productName,
            stock = detail.stock.toInt(),
            price = detail.price,
            currency = detail.currency
        )
    }
}


data class RegisterRequest(val productName: String, val price: Long, val currency: String)
data class RegisterResponse(val productId: UUID, val productName: String)

data class ProductSummaryResponse(val productId: UUID, val productName: String)
data class ProductDetailResponse(
    val productId: UUID,
    val productName: String,
    val stock: Int,
    val price: Long,
    val currency: String
)
