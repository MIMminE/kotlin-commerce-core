package nuts.commerce.productservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericToStringSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.util.UUID

@Configuration
class RedisConfig {

    @Bean
    fun stockRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<UUID, Long> {
        return RedisTemplate<UUID, Long>().apply {
            setConnectionFactory(connectionFactory)

            keySerializer = GenericToStringSerializer(UUID::class.java)
            valueSerializer = GenericToStringSerializer(Long::class.java)
            hashKeySerializer = StringRedisSerializer()
            hashValueSerializer = StringRedisSerializer()
            afterPropertiesSet()
        }
    }
}