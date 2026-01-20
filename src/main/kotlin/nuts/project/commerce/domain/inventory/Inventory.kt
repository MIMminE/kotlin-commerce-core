package nuts.project.commerce.domain.inventory

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import nuts.project.commerce.domain.common.BaseEntity
import java.util.UUID

@Entity
@Table(name = "inventory")
class Inventory protected constructor() : BaseEntity() {

    @Id
    @Column(name = "product_id", nullable = false, columnDefinition = "uuid")
    var productId: UUID = UUID.randomUUID()
        protected set

    @Column(name = "available_qty", nullable = false)
    var availableQty: Long = 0
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    companion object {
        fun create(productId: UUID, initialQty: Long): Inventory {
            require(initialQty >= 0) { "Initial quantity must be non-negative" }
            return Inventory().apply {
                this.productId = productId
                this.availableQty = initialQty
            }
        }
    }

    fun increase(qty: Long) {
        require(qty > 0) { "Increase quantity must be positive" }
        this.availableQty += qty
    }

    fun decrease(qty: Long) {
        require(qty > 0) { "Decrease quantity must be positive" }
        require(this.availableQty >= qty) { "Insufficient inventory to decrease by $qty" }
        this.availableQty -= qty
    }

    /**
     * 수량 업데이트 부분은 동시성 차감을 고려해야 한다.
     */
}