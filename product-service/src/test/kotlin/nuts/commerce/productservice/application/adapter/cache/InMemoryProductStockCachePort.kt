package nuts.commerce.productservice.application.adapter.cache

import nuts.commerce.productservice.port.cache.ProductStockCachePort
import nuts.commerce.productservice.port.cache.StockQuantity
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 테스트/개발용 인메모리 구현체입니다.
 * 필요한 경우 테스트에서 직접 초기화(setStock)하거나 adjustStock 등으로 상태를 변경해서 사용하세요.
 */
class InMemoryProductStockCachePort : ProductStockCachePort {

    private val store: ConcurrentHashMap<UUID, StockQuantity> = ConcurrentHashMap()

    override fun getStock(productId: UUID): StockQuantity {
        // 존재하지 않으면 0으로 초기화된 StockQuantity를 반환합니다.
        return store.computeIfAbsent(productId) { StockQuantity(0L, Instant.now()) }
    }

    /**
     * 테스트에서 사용하는 헬퍼: 특정 productId의 수량을 설정합니다.
     */
    fun setStock(productId: UUID, quantity: Long) {
        store[productId] = StockQuantity(quantity, Instant.now())
    }

    /**
     * 재고 수량을 delta만큼 조정합니다(증가/감소). 음수로 전달하면 감소합니다.
     */
    fun adjustStock(productId: UUID, delta: Long): StockQuantity {
        return store.compute(productId) { _, current ->
            val now = Instant.now()
            val currentQty = current?.quantity ?: 0L
            StockQuantity(currentQty + delta, now)
        }!!
    }

    /** 테스트용: 캐시 초기화 */
    fun clear() = store.clear()

    /** 테스트용: 내부 상태 복사 반환 */
    fun snapshot(): Map<UUID, StockQuantity> = HashMap(store)
}