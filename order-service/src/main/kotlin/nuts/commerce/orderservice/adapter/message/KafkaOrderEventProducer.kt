package nuts.commerce.orderservice.adapter.message

import nuts.commerce.orderservice.exception.OrderException
import nuts.commerce.orderservice.port.message.OrderEventProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

@Component
class KafkaOrderEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val props: OrderEventTopicProperties

) : OrderEventProducer {
    override fun produce(produceEvent: OrderEventProducer.ProduceEvent) {
        val eventId = produceEvent.eventId
        val eventType = produceEvent.eventType
        val payload = produceEvent.payload
        val aggregateId = produceEvent.aggregateId

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