package nuts.commerce.productservice.adapter.web

import nuts.commerce.productservice.exception.ProductException
import nuts.commerce.productservice.usecase.ActivateProductUseCase
import nuts.commerce.productservice.usecase.DeactivateProductUseCase
import nuts.commerce.productservice.usecase.DeleteProductUseCase
import nuts.commerce.productservice.usecase.GetProductDetailUseCase
import nuts.commerce.productservice.usecase.GetProductsUseCase
import nuts.commerce.productservice.usecase.RegisterProductUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val registerProductUseCase: RegisterProductUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val getProductDetailUseCase: GetProductDetailUseCase,
    private val activateProductUseCase: ActivateProductUseCase,
    private val deactivateProductUseCase: DeactivateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase
) {

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

    @PostMapping("/{id}/activate")
    fun activate(@PathVariable id: UUID): ResponseEntity<ActionResult> {
        val res = activateProductUseCase.execute(id)
        return ResponseEntity.ok(ActionResult(res.productId))
    }

    @PostMapping("/{id}/deactivate")
    fun deactivate(@PathVariable id: UUID): ResponseEntity<ActionResult> {
        val res = deactivateProductUseCase.execute(id)
        return ResponseEntity.ok(ActionResult(res.productId))
    }

    @PostMapping("/{id}/delete")
    fun delete(@PathVariable id: UUID): ResponseEntity<ActionResult> {
        val res = deleteProductUseCase.execute(id)
        return ResponseEntity.ok(ActionResult(res.productId))
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

data class ActionResult(val productId: UUID)