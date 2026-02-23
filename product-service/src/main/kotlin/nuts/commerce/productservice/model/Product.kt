package nuts.commerce.productservice.model

import jakarta.persistence.*
import nuts.commerce.productservice.exception.ProductException
import java.util.*

@Entity
@Table(
    name = "products",
    uniqueConstraints = [UniqueConstraint(columnNames = ["productName", "idempotency_key"])]
)
class Product protected constructor(

    @Id
    @Column(nullable = false, updatable = false)
    val productId: UUID,

    @Column(nullable = false)
    var productName: String,

    @Column(nullable = false, updatable = false)
    val idempotencyKey: UUID,

    @Embedded
    var price: Money,

    @Version
    var version: Long? = null

) : BaseEntity() {

    companion object {
        fun create(
            productId: UUID = UUID.randomUUID(),
            productName: String,
            idempotencyKey: UUID,
            price: Money,
        ): Product {
            return Product(
                productId = productId,
                productName = productName,
                idempotencyKey = idempotencyKey,
                price = price,
            )
        }
    }
}