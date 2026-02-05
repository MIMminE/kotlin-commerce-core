package nuts.commerce.productservice.application.port.repository

import nuts.commerce.productservice.model.exception.ProductException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryStockQuery(
    initial: Map<UUID, Int> = emptyMap()
) : StockQuery {

    private val store: MutableMap<UUID, Int> = ConcurrentHashMap()

    init {
        store.putAll(initial)
    }

    override fun getStockQuantity(productId: UUID): Int = store[productId] ?: 0

    fun setStock(productId: UUID, qty: Int) {
        if (qty < 0) throw ProductException.InvalidCommand("qty must be >= 0")
        store[productId] = qty
    }

    fun increase(productId: UUID, qty: Int) {
        if (qty <= 0) throw ProductException.InvalidCommand("qty must be > 0")
        store.merge(productId, qty) { a, b -> a + b }
    }

    fun decrease(productId: UUID, qty: Int) {
        if (qty <= 0) throw ProductException.InvalidCommand("qty must be > 0")
        val cur = store.getOrDefault(productId, 0)
        if (cur < qty) throw ProductException.InvalidCommand("not enough stock")
        store[productId] = cur - qty
    }

    fun clear() = store.clear()
}
