package nuts.commerce.productservice.model.domain

import jakarta.persistence.*
import nuts.commerce.productservice.model.BaseEntity
import nuts.commerce.productservice.model.exception.ProductException
import java.util.*

@Entity
@Table(
    name = "products",

    )
class Product protected constructor(

    @Id
    @Column(nullable = false, updatable = false)
    val productId: UUID,

    @Column(nullable = false)
    var productName: String,

    @Embedded
    @Column(nullable = false)
    var price: Money,

    @Enumerated(EnumType.STRING)
    var status: ProductStatus,

    @Version
    var version: Long? = null

) : BaseEntity() {

    fun activate() {
        if (status != ProductStatus.INACTIVE) {
            throw ProductException.InvalidTransition(
                productId = productId,
                from = status,
                to = ProductStatus.ACTIVE
            )
        }
        status = ProductStatus.ACTIVE
    }

    fun deactivate() {
        if (status != ProductStatus.ACTIVE) {
            throw ProductException.InvalidTransition(
                productId = productId,
                from = status,
                to = ProductStatus.INACTIVE
            )
        }
        status = ProductStatus.INACTIVE
    }

    fun delete() {
        if (status == ProductStatus.DELETED) {
            throw ProductException.InvalidTransition(
                productId = productId,
                from = status,
                to = ProductStatus.DELETED
            )
        }
        status = ProductStatus.DELETED
    }

    fun updatePrice(newPrice: Money) {
        if (newPrice.amount < 0) {
            throw ProductException.InvalidCommand("price cannot be negative")
        }
        this.price = newPrice
    }

    fun updateName(newName: String) {
        if (newName.isBlank()) {
            throw ProductException.InvalidCommand("product name cannot be blank")
        }
        this.productName = newName
    }

    companion object {
        fun create(
            productId: UUID = UUID.randomUUID(),
            productName: String,
            price: Money,
            status: ProductStatus = ProductStatus.ACTIVE,
        ): Product {
            return Product(
                productId = productId,
                productName = productName,
                price = price,
                status = status,
            )
        }
    }

    enum class ProductStatus {
        ACTIVE,
        INACTIVE,
        DELETED
    }
}