package nuts.commerce.orderservice.application.adapter.message

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "order.events.kafka")
data class OrderEventTopicProperties(
    val topic: String,
)