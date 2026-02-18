package nuts.commerce.paymentservice.usecase

import nuts.commerce.paymentservice.event.InboundEventType
import nuts.commerce.paymentservice.event.OutboundEventType
import nuts.commerce.paymentservice.event.PaymentConfirmPayload
import nuts.commerce.paymentservice.event.PaymentConfirmSuccessPayload
import nuts.commerce.paymentservice.event.PaymentInboundEvent
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
class PaymentConfirmedUseCase(
    private val paymentRepository: PaymentRepository,
    private val outboxRepository: OutboxRepository,
    private val paymentProvider: PaymentProvider,
    private val txTemplate: TransactionTemplate,
    private val objectMapper: ObjectMapper
) {
    fun execute(command: PaymentConfirmCommand): PaymentConfirmResult {

        val providerPaymentId = paymentRepository.getProviderPaymentIdByPaymentId(command.paymentId)
            ?: throw IllegalStateException("Provider payment ID not found for payment ID: ${command.paymentId}")

        paymentProvider.commitPayment(providerPaymentId, command.eventId) // 이벤트 아이디를 통한 멱등성 방어
            .whenComplete { result, _ ->
                when (result) {
                    is PaymentStatusUpdateResult.Success -> confirmRequestSuccessHandler(
                        result,
                        command.orderId,
                        command.paymentId,
                        command.eventId
                    )
                }
            }
        return PaymentConfirmResult(providerPaymentId)
    }

    private fun confirmRequestSuccessHandler(
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

data class PaymentConfirmCommand(val orderId: UUID, val paymentId: UUID, val eventId: UUID) {
    companion object {
        fun from(inboundEvent: PaymentInboundEvent): PaymentConfirmCommand {
            require(inboundEvent.eventType == InboundEventType.PAYMENT_CONFIRM)
            require(inboundEvent.payload is PaymentConfirmPayload)

            val payload = inboundEvent.payload
            return PaymentConfirmCommand(
                orderId = inboundEvent.orderId,
                paymentId = payload.paymentId,
                eventId = inboundEvent.eventId
            )
        }
    }
}

data class PaymentConfirmResult(val providerPaymentId: UUID)