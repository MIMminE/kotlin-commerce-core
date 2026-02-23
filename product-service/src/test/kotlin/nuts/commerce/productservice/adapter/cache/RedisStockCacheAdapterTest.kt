package nuts.commerce.productservice.adapter.cache

import nuts.commerce.productservice.config.RedisConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.redis.core.RedisTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.*

@Suppress("NonAsciiCharacters")
@SpringBootTest(
    classes =
        [RedisConfig::class, RedisStockCacheAdapter::class]
)
@Testcontainers
@ImportAutoConfiguration(DataRedisAutoConfiguration::class)
class RedisStockCacheAdapterTest {

    @Autowired
    lateinit var adapter: RedisStockCacheAdapter

    @Autowired
    lateinit var redisTemplate: RedisTemplate<UUID, Long>

    companion object {
        @Container
        @ServiceConnection
        val redis = GenericContainer(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379)
    }

    @BeforeEach
    fun clear() {
        redisTemplate.connectionFactory?.connection?.serverCommands()?.flushDb()
    }

    @Test
    fun `saveStock 후 getStock 조회된다`() {
        val id = UUID.randomUUID()

        adapter.saveStock(id, 10L)

        assertEquals(10L, adapter.getStock(id))
    }

    @Test
    fun `없는 키 getStock 은 예외`() {
        val id = UUID.randomUUID()

        assertThrows(IllegalStateException::class.java) {
            adapter.getStock(id)
        }
    }

    @Test
    fun `plusStock 은 기존 키가 있어야 하고 증가한다`() {
        val id = UUID.randomUUID()
        adapter.saveStock(id, 10L)

        adapter.plusStock(id, 5L)

        assertEquals(15L, adapter.getStock(id))
    }

    @Test
    fun `minusStock 은 기존 키가 있어야 하고 감소한다`() {
        val id = UUID.randomUUID()
        adapter.saveStock(id, 10L)

        adapter.minusStock(id, 3L)

        assertEquals(7L, adapter.getStock(id))
    }

    @Test
    fun `plusStock 에 음수면 예외`() {
        val id = UUID.randomUUID()
        adapter.saveStock(id, 10L)

        assertThrows(IllegalArgumentException::class.java) {
            adapter.plusStock(id, -1L)
        }
    }

    @Test
    fun `minusStock 에 음수면 예외`() {
        val id = UUID.randomUUID()
        adapter.saveStock(id, 10L)

        assertThrows(IllegalArgumentException::class.java) {
            adapter.minusStock(id, -1L)
        }
    }

    @Test
    fun `getStocks 는 모두 존재할 때만 Map 으로 반환`() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        adapter.saveStock(id1, 10L)
        adapter.saveStock(id2, 20L)

        val result = adapter.getStocks(listOf(id1, id2))

        assertEquals(mapOf(id1 to 10L, id2 to 20L), result)
    }

    @Test
    fun `getStocks 에 누락이 있으면 예외`() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        adapter.saveStock(id1, 10L)

        assertThrows(IllegalStateException::class.java) {
            adapter.getStocks(listOf(id1, id2))
        }
    }
}