package nuts.commerce.orderservice.adapter.message

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "order.events.kafka")
data class OrderEventTopicProperties(
    val topic: String,
)