package nuts.commerce.productservice.adapter.web

import jakarta.persistence.OptimisticLockException
import nuts.commerce.productservice.exception.ProductException
import nuts.commerce.productservice.usecase.GetProductDetailUseCase
import nuts.commerce.productservice.usecase.GetProductsUseCase
import nuts.commerce.productservice.usecase.RegisterProductUseCase
import nuts.commerce.productservice.usecase.UpdateProductPriceUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val updateProductPriceUseCase: UpdateProductPriceUseCase,
    private val registerProductUseCase: RegisterProductUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val getProductDetailUseCase: GetProductDetailUseCase,
) {

    @PutMapping("/price")
    fun updatePrice(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: UpdatePriceRequest
    ): ResponseEntity<UpdatePriceResponse> {

        val username = userDetails.username
        val productId = request.productId

        return try {
            updateProductPriceUseCase.execute(username, productId, request.price, request.currency)
            ResponseEntity.ok(UpdatePriceResponse(productId))
        } catch (e: OptimisticLockException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                    UpdatePriceResponse(
                        productId,
                        "The product was updated by another transaction. Please try again."
                    )
                )
        } catch (e: ProductException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(UpdatePriceResponse(productId, e.message ?: "An error occurred while updating the product price"))

        }
    }

    @PostMapping
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse> {
        val cmd = RegisterProductUseCase.RegisterProductCommand(
            productName = request.productName,
            price = request.price,
            currency = request.currency
        )
        val res = registerProductUseCase.execute(cmd)
        return ResponseEntity.status(HttpStatus.CREATED).body(RegisterResponse(res.productId, res.productName))
    }

    @GetMapping
    fun list(): List<ProductSummaryResponse> {
        return getProductsUseCase.execute().map { ProductSummaryResponse(it.productId, it.productName) }
    }

    @GetMapping("/{id}")
    fun detail(@PathVariable id: UUID): ResponseEntity<ProductDetailResponse> {
        val detail = getProductDetailUseCase.execute(id)
        return ResponseEntity.ok(
            ProductDetailResponse(
                productId = detail.productId,
                productName = detail.productName,
                stock = detail.stock.toInt(),
                price = detail.price,
                currency = detail.currency
            )
        )
    }
}

data class UpdatePriceRequest(val productId: UUID, val price: Long, val currency: String)
data class UpdatePriceResponse(val productId: UUID, val message: String? = null)

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
