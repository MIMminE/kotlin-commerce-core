package nuts.commerce.paymentservice.event.inbound.handler

import nuts.commerce.paymentservice.event.inbound.InboundEventType
import nuts.commerce.paymentservice.event.inbound.PaymentConfirmPayload
import nuts.commerce.paymentservice.event.inbound.PaymentInboundEvent
import nuts.commerce.paymentservice.event.outbound.OutboundEventType
import nuts.commerce.paymentservice.event.outbound.PaymentConfirmSuccessPayload
import nuts.commerce.paymentservice.model.OutboxRecord
import nuts.commerce.paymentservice.port.payment.PaymentProvider
import nuts.commerce.paymentservice.port.payment.PaymentStatusUpdateResult
import nuts.commerce.paymentservice.port.repository.OutboxRepository
import nuts.commerce.paymentservice.port.repository.PaymentRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Component
class PaymentConfirmRequestHandler(
    private val paymentRepository: PaymentRepository,
    private val outboxRepository: OutboxRepository,
    private val paymentProvider: PaymentProvider,
    private val txTemplate: TransactionTemplate,
    private val objectMapper: ObjectMapper
) : PaymentEventHandler {
    override val supportType: InboundEventType
        get() = InboundEventType.PAYMENT_CONFIRM

    override fun handle(paymentInboundEvent: PaymentInboundEvent) {
        val eventId = paymentInboundEvent.eventId
        val orderId = paymentInboundEvent.orderId
        val payload = paymentInboundEvent.payload as PaymentConfirmPayload

        val providerPaymentId = paymentRepository.getProviderPaymentIdByPaymentId(payload.paymentId)
            ?: throw IllegalStateException("Provider payment ID not found for payment ID: ${payload.paymentId}")

        paymentProvider.commitPayment(providerPaymentId, eventId) // 이벤트 아이디를 통한 멱등성 방어
            .whenComplete { result, _ ->
                when (result) {
                    is PaymentStatusUpdateResult.Success -> successHandle(
                        result = result,
                        orderId = orderId,
                        paymentId = payload.paymentId,
                        eventId = eventId
                    )
                }
            }
    }

    private fun successHandle(
        result: PaymentStatusUpdateResult.Success,
        orderId: UUID,
        paymentId: UUID,
        eventId: UUID
    ) {
        txTemplate.execute {

            val payment = paymentRepository.findById(paymentId)!!
            payment.commit()

            val payload = PaymentConfirmSuccessPayload(
                paymentProvider = paymentProvider.providerName,
                providerPaymentId = result.providerPaymentId
            )

            val outboxRecord = OutboxRecord.create(
                orderId = orderId,
                paymentId = paymentId,
                idempotencyKey = eventId,
                eventType = OutboundEventType.PAYMENT_CONFIRM,
                payload = objectMapper.writeValueAsString(payload)
            )

            outboxRepository.save(outboxRecord)
        }
    }
}