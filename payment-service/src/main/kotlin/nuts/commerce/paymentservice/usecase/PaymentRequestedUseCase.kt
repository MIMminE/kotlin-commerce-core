package nuts.commerce.paymentservice.usecase

import nuts.commerce.paymentservice.model.EventType
import nuts.commerce.paymentservice.model.Money
import nuts.commerce.paymentservice.model.OutboxRecord
import nuts.commerce.paymentservice.model.Payment
import nuts.commerce.paymentservice.port.payment.ChargeRequest
import nuts.commerce.paymentservice.port.payment.ChargeResult
import nuts.commerce.paymentservice.port.payment.PaymentProvider
import nuts.commerce.paymentservice.port.repository.OutboxRepository
import nuts.commerce.paymentservice.port.repository.PaymentRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Component
class PaymentRequestedUseCase(
    private val paymentRepository: PaymentRepository,
    private val outboxRepository: OutboxRepository,
    private val paymentProvider: PaymentProvider,
    private val txTemplate: TransactionTemplate,
    private val objectMapper: ObjectMapper
) {

    fun execute(command: PaymentRequestCommand): Result {
        val paymentId =
            try {
                txTemplate.execute {
                    paymentRepository.save(
                        Payment.create(
                            orderId = command.orderId,
                            idempotencyKey = command.eventId,
                            money = Money(amount = command.amount, currency = command.currency)
                        )
                    )
                }
            } catch (ex: DataIntegrityViolationException) {
                val existingPaymentId =
                    paymentRepository.findPaymentIdForIdempotencyKey(command.orderId, command.eventId)
                        ?: throw ex
                return Result(existingPaymentId)
            }

        val chargeRequest = ChargeRequest(
            orderId = command.orderId,
            paymentId = paymentId,
            amount = command.amount,
            currency = command.currency
        )

        paymentProvider.charge(chargeRequest)
            .whenComplete { result, ex ->
                if (ex != null) {
                    when (result) {
                        is ChargeResult.Success -> chargeRequestSuccessHandler(result, command.orderId, command.eventId)
                        is ChargeResult.Failure -> chargeRequestFailureHandler(result, command.orderId, command.eventId)
                    }
                }
            }
        return Result(paymentId)
    }

    private fun chargeRequestSuccessHandler(result: ChargeResult.Success, orderId: UUID, eventId: UUID) {
        txTemplate.execute {
            val payment = paymentRepository.findById(paymentId = result.requestPaymentId)!!
            payment.approve(
                paymentProvider = paymentProvider.providerName,
                providerPaymentId = result.providerPaymentId
            )

            val payload = objectMapper.writeValueAsString(
                mapOf(
                    "paymentId" to result.requestPaymentId,
                    "orderId" to orderId,
                    "paymentProvider" to paymentProvider.providerName,
                )
            )

            val outboxRecord = OutboxRecord.create(
                orderId = orderId,
                paymentId = result.requestPaymentId,
                idempotencyKey = eventId,
                eventType = EventType.PAYMENT_CREATION,
                payload = payload
            )

            outboxRepository.save(outboxRecord)
        }
    }

    private fun chargeRequestFailureHandler(result: ChargeResult.Failure, orderId: UUID, eventId: UUID) {
        txTemplate.execute {
            val payment = paymentRepository.findById(paymentId = result.requestPaymentId)!!
            payment.fail(reason = result.reason)

            val payload = objectMapper.writeValueAsString(
                mapOf(
                    "paymentId" to result.requestPaymentId,
                    "orderId" to orderId,
                    "failureReason" to result.reason
                )
            )

            val outboxRecord = OutboxRecord.create(
                orderId = orderId,
                paymentId = result.requestPaymentId,
                idempotencyKey = eventId,
                eventType = EventType.PAYMENT_CREATION_FAILED,
                payload = payload
            )

            outboxRepository.save(outboxRecord)
        }
    }
}


data class Result(val paymentId: UUID)

data class PaymentRequestCommand(
    val orderId: UUID,
    val eventId: UUID,
    val amount: Long,
    val currency: String = "KRW",
)