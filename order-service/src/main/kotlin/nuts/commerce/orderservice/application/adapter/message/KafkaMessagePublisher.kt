package nuts.commerce.orderservice.application.adapter.message

import nuts.commerce.orderservice.model.domain.exception.OrderException
import nuts.commerce.orderservice.application.port.message.MessagePublisher
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class KafkaMessagePublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val props: OrderEventTopicProperties

) : MessagePublisher {
    override fun publish(
        eventId: UUID,
        eventType: String,
        payload: String,
        aggregateId: UUID
    ) {
        val topic = props.topic
        val record = ProducerRecord(topic, aggregateId.toString(), payload).apply {
            headers().add("eventId", eventId.toString().toByteArray(StandardCharsets.UTF_8))
            headers().add("eventType", eventType.toByteArray(StandardCharsets.UTF_8))
            headers().add("aggregateId", aggregateId.toString().toByteArray(StandardCharsets.UTF_8))
        }
        try {
            kafkaTemplate.send(record).get(3, TimeUnit.SECONDS)
        } catch (ex: Exception) {
            val cause = ex.cause ?: ex
            throw OrderException.MessagePublishFailed(
                message = "Failed to publish order event to kafka. topic=$topic",
                cause = cause,
                eventId = eventId,
                aggregateId = aggregateId,
                eventType = eventType
            )
        }
    }
}