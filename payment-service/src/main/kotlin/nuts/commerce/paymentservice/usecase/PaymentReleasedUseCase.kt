package nuts.commerce.paymentservice.usecase

import nuts.commerce.paymentservice.model.EventType
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
class PaymentReleasedUseCase(
    private val paymentRepository: PaymentRepository,
    private val outboxRepository: OutboxRepository,
    private val paymentProvider: PaymentProvider,
    private val txTemplate: TransactionTemplate,
    private val objectMapper: ObjectMapper
) {

    fun execute(command: PaymentReleaseCommand) {

        val providerPaymentId = paymentRepository.getProviderPaymentIdByPaymentId(command.paymentId)
            ?: throw IllegalStateException("Provider payment ID not found for payment ID: ${command.paymentId}")

        paymentProvider.releasePayment(providerPaymentId, command.eventId) // 이벤트 아이디를 통한 멱등성 방어
            .whenComplete { result, _ ->
                when (result) {
                    is PaymentStatusUpdateResult.Success -> releaseRequestSuccessHandler(
                        result,
                        command.orderId,
                        command.paymentId,
                        command.eventId
                    )

                    is PaymentStatusUpdateResult.Failure -> releaseRequestFailureHandler(
                        result,
                        command.orderId,
                        command.paymentId,
                        command.eventId
                    )
                }
            }
    }

    private fun releaseRequestSuccessHandler(
        result: PaymentStatusUpdateResult.Success,
        orderId: UUID,
        paymentId: UUID,
        eventId: UUID
    ) {
        txTemplate.execute {

            val payment = paymentRepository.findById(paymentId)!!
            payment.release()

            val payload = objectMapper.writeValueAsString(
                mapOf(
                    "paymentId" to paymentId,
                    "orderId" to orderId,
                    "paymentProvider" to paymentProvider.providerName,
                    "providerPaymentId" to result.providerPaymentId
                )
            )

            val outboxRecord = OutboxRecord.create(
                orderId = orderId,
                paymentId = paymentId,
                idempotencyKey = eventId,
                eventType = EventType.PAYMENT_RELEASED,
                payload = payload
            )

            outboxRepository.save(outboxRecord)
        }
    }

    private fun releaseRequestFailureHandler(
        result: PaymentStatusUpdateResult.Failure,
        orderId: UUID,
        paymentId: UUID,
        eventId: UUID
    ) {
        txTemplate.execute {

            val payment = paymentRepository.findById(paymentId)!!
            payment.fail(result.reason)

            val payload = objectMapper.writeValueAsString(
                mapOf(
                    "paymentId" to paymentId,
                    "orderId" to orderId,
                    "paymentProvider" to paymentProvider.providerName,
                    "providerPaymentId" to result.providerPaymentId,
                    "failureReason" to result.reason
                )
            )

            val outboxRecord = OutboxRecord.create(
                orderId = orderId,
                paymentId = paymentId,
                idempotencyKey = eventId,
                eventType = EventType.PAYMENT_RELEASE_FAILED,
                payload = payload
            )

            outboxRepository.save(outboxRecord)
        }
    }
}

data class PaymentReleaseCommand(val orderId: UUID, val paymentId: UUID, val eventId: UUID)