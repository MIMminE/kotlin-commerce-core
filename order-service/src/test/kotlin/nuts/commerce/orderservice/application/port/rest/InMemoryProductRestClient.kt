package nuts.commerce.orderservice.application.port.rest

import nuts.commerce.orderservice.port.rest.ProductPriceSnapshot
import nuts.commerce.orderservice.port.rest.ProductRestClient
import java.util.concurrent.ConcurrentHashMap

class InMemoryProductRestClient(private val defaultCurrency: String = "KRW") : ProductRestClient {

    private val store: ConcurrentHashMap<String, ProductPriceSnapshot> = ConcurrentHashMap()

    var snapshots: Map<String, ProductPriceSnapshot>
        get() = store.toMap()
        set(value) {
            store.clear()
            value.values.forEach { store[it.productId] = it }
        }

    override fun getPriceSnapshots(productIds: List<String>): List<ProductPriceSnapshot> {
        return productIds.map { id -> store[id] ?: ProductPriceSnapshot(id, 0L, defaultCurrency) }
    }

    fun put(snapshot: ProductPriceSnapshot) {
        store[snapshot.productId] = snapshot
    }

    fun put(productId: String, price: Long, currency: String = defaultCurrency) {
        store[productId] = ProductPriceSnapshot(productId, price, currency)
    }

    fun putAll(snapshots: Iterable<ProductPriceSnapshot>) {
        snapshots.forEach { put(it) }
    }

    fun clear() {
        store.clear()
    }

    fun remove(productId: String) {
        store.remove(productId)
    }
}
