package nuts.commerce.orderservice.adapter.message

import nuts.commerce.orderservice.port.message.OrderEvent
import nuts.commerce.orderservice.port.message.OrderEventProducer
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class KafkaOrderEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value($$"${order.kafka.producer.topic:order-outbound}")
    private val orderTopic: String,
    private val objectMapper: ObjectMapper

    ) : OrderEventProducer {
    override fun produce(orderEvent: OrderEvent) {
        TODO("Not yet implemented")
    }
}