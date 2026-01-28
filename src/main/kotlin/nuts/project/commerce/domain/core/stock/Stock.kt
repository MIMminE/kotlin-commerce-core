package nuts.project.commerce.domain.core.stock

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import nuts.project.commerce.domain.common.BaseEntity
import java.util.UUID

@Entity
@Table(
    name = "stock",
    indexes = [
        Index(name = "idx_product_id", columnList = "product_id")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_stock_product_id", columnNames = ["product_id"])
    ]
)
class Stock protected constructor() : BaseEntity() {

    @Id
    @Column(name = "stock_id", nullable = false, columnDefinition = "uuid")
    lateinit var id: UUID
        protected set

    @Column(name = "product_id", nullable = false, columnDefinition = "uuid")
    lateinit var productId: UUID
        protected set

    @Column(name = "on_hand_qty", nullable = false)
    var onHandQty: Long = 0
        protected set

    @Column(name = "reserved_qty", nullable = false)
    var reservedQty: Long = 0
        protected set


    fun reserve(qty: Long) {
        require(qty > 0) { "Reserve quantity must be positive" }
        require(this.onHandQty >= qty) { "Insufficient inventory to reserve $qty" }
//        this.onHandQty -= qty
        this.reservedQty += qty
    }

    fun confirm(qty: Long) {
        require(qty > 0) { "Confirm quantity must be positive" }
        require(this.reservedQty >= qty) { "Insufficient reserved inventory to confirm $qty" }
        this.onHandQty -= qty
        this.reservedQty -= qty
    }

    fun release(qty: Long) {
        require(qty > 0) { "Release quantity must be positive" }
        require(this.reservedQty >= qty) { "Insufficient reserved inventory to release $qty" }
        this.reservedQty -= qty
        this.onHandQty += qty
    }

    fun increase(qty: Long) {
        require(qty > 0) { "Increase quantity must be positive" }
        this.onHandQty += qty
    }

    fun decrease(qty: Long) {
        require(qty > 0) { "Decrease quantity must be positive" }
        require(this.onHandQty >= qty) { "Insufficient inventory to decrease by $qty" }
        this.onHandQty -= qty
    }

    companion object {
        fun create(id: UUID, productId: UUID, initialQty: Long): Stock {
            require(initialQty >= 0) { "Initial quantity must be non-negative" }
            return Stock().apply {
                this.id = id
                this.productId = productId
                this.onHandQty = initialQty
            }
        }
    }
}