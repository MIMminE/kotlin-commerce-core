package nuts.commerce.paymentservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableConfigurationProperties(OutboxExecutorProperties::class)
class AsyncConfig(
    private val props: OutboxExecutorProperties
) {
    @Bean(name = ["outboxUpdateExecutor"])
    fun outboxUpdateExecutor(): Executor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = props.corePoolSize
            maxPoolSize = props.maxPoolSize
            queueCapacity = props.queueCapacity
            setThreadNamePrefix("outbox-update-")
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(props.awaitTerminationSeconds)
            initialize()
        }
}

@ConfigurationProperties(prefix = "system.outbox-publisher.executor")
data class OutboxExecutorProperties(
    var corePoolSize: Int = 4,
    var maxPoolSize: Int = 8,
    var queueCapacity: Int = 500,
    var awaitTerminationSeconds: Int = 10,
)