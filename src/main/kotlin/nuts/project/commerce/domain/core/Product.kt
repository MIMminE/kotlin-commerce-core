package nuts.project.commerce.domain.core

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import nuts.project.commerce.domain.common.StockPolicyType

@Entity
@Table(name = "products")
class Product(

    @Id
    var id: Long? = null,

    var name: String = "",

    var stock: Int = 0,

    @Enumerated(EnumType.STRING)
    var stockPolicyType: StockPolicyType = StockPolicyType.RESERVE_ON_ORDER,

    @Version
    var version: Long? = null
) {
    fun decreaseStock(qty: Int) {
        require(qty > 0) { "qty must be positive" }
        check(stock >= qty) { "OUT_OF_STOCK" }
        stock -= qty
    }

    fun increaseStock(qty: Int) {
        require(qty > 0) { "qty must be positive" }
        stock += qty
    }
}