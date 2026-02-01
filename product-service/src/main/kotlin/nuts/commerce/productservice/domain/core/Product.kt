package nuts.commerce.productservice.domain.core

import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import nuts.commerce.productservice.domain.BaseEntity
import nuts.commerce.productservice.domain.Money
import nuts.commerce.productservice.domain.ProductStatus
import java.util.UUID


@Entity
@Table(
    name = "products",
    indexes = [
        Index(name = "ix_products_status", columnList = "status"),
        Index(name = "ix_products_name", columnList = "name")
    ]
)
class Product protected constructor() : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    lateinit var id: UUID
        protected set

    @Column(name = "name", nullable = false, length = 120)
    lateinit var name: String
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: ProductStatus = ProductStatus.ACTIVE
        protected set

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount", column = Column(name = "price_amount", nullable = false)),
        AttributeOverride(name = "currency", column = Column(name = "price_currency", nullable = false, length = 8))
    )
    lateinit var price: Money
        protected set

    @Embedded
    var stock: Stock = Stock(available = 0, reserved = 0)
        protected set

    companion object {
        fun create(
            name: String,
            price: Money,
            initialStock: Int = 0,
            idGenerator: () -> UUID = { UUID.randomUUID() }
        ): Product {
            require(name.isNotBlank()) { "name is required" }
            require(price.amount >= 0L) { "price must be >= 0" }
            require(initialStock >= 0) { "initialStock must be >= 0" }

            return Product().apply {
                this.id = idGenerator()
                this.name = name
                this.price = price
                this.status = ProductStatus.ACTIVE
                this.stock = Stock(available = initialStock, reserved = 0)
            }
        }
    }

    fun activate() {
        this.status = ProductStatus.ACTIVE
    }

    fun deactivate() {
        this.status = ProductStatus.INACTIVE
    }

    fun increaseStock(qty: Int) {
        stock.increase(qty)
    }

    fun reserveStock(qty: Int) {
        require(status == ProductStatus.ACTIVE) { "product is not active" }
        stock.reserve(qty)
    }

    fun releaseReservedStock(qty: Int) {
        stock.release(qty)
    }

    fun deductReservedStock(qty: Int) {
        stock.deductReserved(qty)
    }
}