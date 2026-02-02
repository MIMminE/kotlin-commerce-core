package nuts.commerce.orderservice.application.adapter.trigger

import nuts.commerce.orderservice.application.usecase.OnPaymentApprovedUseCase
import nuts.commerce.orderservice.application.usecase.OnPaymentFailedUseCase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.UUID

@Component
class PaymentResultKafkaListener(
    private val onPaymentApprovedUseCase: OnPaymentApprovedUseCase,
    private val onPaymentFailedUseCase: OnPaymentFailedUseCase
) {
    @KafkaListener(topics = ["\${payment.result.topic}"])
    fun onMessage(record: ConsumerRecord<String, String>) {
        val eventId = record.headerUuid("eventId")
        val eventType = record.headerString("eventType")
        val aggregateId = record.headerUuid("aggregateId") // 여기서는 orderId로 사용
        val payload = record.value()
    }

    private fun ConsumerRecord<String, String>.headerString(name: String): String =
        headers().lastHeader(name)?.value()
            ?.toString(StandardCharsets.UTF_8)
            ?: throw IllegalArgumentException("Missing kafka header: $name")

    private fun ConsumerRecord<String, String>.headerUuid(name: String): UUID =
        UUID.fromString(headerString(name))
}