package nuts.project.commerce.domain.product

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import nuts.project.commerce.domain.common.BaseEntity
import nuts.project.commerce.domain.common.Money
import java.util.UUID

@Entity
@Table(name = "product")
class Product protected constructor() : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID()
        protected set


    @Column(name = "name", nullable = false, length = 120)
    lateinit var name: String
        protected set


    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "price", nullable = false))
    lateinit var price: Money
        protected set

    @Column(name = "active", nullable = false)
    var active: Boolean = true
        protected set

    companion object {
        fun create(name: String, price: Money, active: Boolean = true): Product {
            require(name.isNotBlank()) { "Product name must not be blank" }
            return Product().apply {
                this.name = name.trim()
                this.price = price
                this.active = active
            }
        }
    }

    fun deactivate() {
        this.active = false
    }
}