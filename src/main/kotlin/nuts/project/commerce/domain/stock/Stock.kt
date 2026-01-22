package nuts.project.commerce.domain.stock

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import nuts.project.commerce.domain.common.BaseEntity
import java.util.UUID

@Entity
@Table(name = "stock")
class Stock protected constructor() : BaseEntity() {

    @Id
    @Column(name = "product_id", nullable = false, columnDefinition = "uuid")
    var productId: UUID = UUID.randomUUID()
        protected set

    @Column(name = "on_hand_qty", nullable = false)
    var onHandQty: Long = 0
        protected set

    @Column(name = "reserved_qty", nullable = false)
    var reservedQty: Long = 0
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    fun reserve(qty: Long) {
        require(qty > 0) { "Reserve quantity must be positive" }
        require(this.onHandQty >= qty) { "Insufficient inventory to reserve $qty" }
        this.onHandQty -= qty
        this.reservedQty += qty
    }

    fun confirm(qty: Long) {
        require(qty > 0) { "Confirm quantity must be positive" }
        require(this.reservedQty >= qty) { "Insufficient reserved inventory to confirm $qty" }
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
        fun create(productId: UUID, initialQty: Long): Stock {
            require(initialQty >= 0) { "Initial quantity must be non-negative" }
            return Stock().apply {
                this.productId = productId
                this.onHandQty = initialQty
            }
        }
    }


}