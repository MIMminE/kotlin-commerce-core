package nuts.commerce.paymentservice.usecase

import nuts.commerce.paymentservice.event.InboundEventType
import nuts.commerce.paymentservice.event.OutboundEventType
import nuts.commerce.paymentservice.event.PaymentCreatePayload
import nuts.commerce.paymentservice.event.PaymentCreationFailPayload
import nuts.commerce.paymentservice.event.PaymentCreationSuccessPayload
import nuts.commerce.paymentservice.event.PaymentInboundEvent
import nuts.commerce.paymentservice.model.Money
import nuts.commerce.paymentservice.model.OutboxRecord
import nuts.commerce.paymentservice.model.Payment
import nuts.commerce.paymentservice.port.payment.CreatePaymentRequest
import nuts.commerce.paymentservice.port.payment.CreatePaymentResult
import nuts.commerce.paymentservice.port.payment.PaymentProvider
import nuts.commerce.paymentservice.port.repository.OutboxRepository
import nuts.commerce.paymentservice.port.repository.PaymentRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Component
class PaymentCreatedUseCase(
    private val paymentRepository: PaymentRepository,
    private val outboxRepository: OutboxRepository,
    private val paymentProvider: PaymentProvider,
    private val objectMapper: ObjectMapper,
    private val txTemplate: TransactionTemplate
) {

    fun execute(command: PaymentCreateCommand): PaymentCreateResult {

        val paymentSaveResult = txTemplate.execute {
            savePaymentWithIdempotencyCheck(command)
        }

        if (!paymentSaveResult.isNewlyCreated) {
            return PaymentCreateResult(paymentSaveResult.paymentId)
        }

        val createPaymentRequest = CreatePaymentRequest(
            orderId = command.orderId,
            paymentId = paymentSaveResult.paymentId,
            amount = command.amount,
            currency = command.currency
        )

        paymentProvider.createPayment(createPaymentRequest)
            .whenComplete { result, ex ->
                if (ex != null) {
                    paymentProviderExceptionHandler(ex, command, paymentSaveResult.paymentId)
                }
                when (result) {
                    is CreatePaymentResult.Success -> requestSuccessHandler(
                        result,
                        command
                    )
                }
            }
        return PaymentCreateResult(paymentSaveResult.paymentId)
    }

    private fun paymentProviderExceptionHandler(ex: Throwable, command: PaymentCreateCommand, paymentId: UUID) {
        txTemplate.execute {
            val exceptionMessage = ex.message ?: "Unknown error occurred during payment creation"

            paymentRepository.findById(paymentId)
                ?.fail(exceptionMessage)

            PaymentCreationFailPayload(
                reason = exceptionMessage
            ).let { payload ->
                val outboxRecord = OutboxRecord.create(
                    orderId = command.orderId,
                    paymentId = paymentId,
                    idempotencyKey = command.eventId,
                    eventType = OutboundEventType.PAYMENT_CREATION_FAILED,
                    payload = objectMapper.writeValueAsString(payload)
                )
                outboxRepository.save(outboxRecord)
            }
        }
    }

    private fun requestSuccessHandler(
        result: CreatePaymentResult.Success,
        command: PaymentCreateCommand,
    ) {
        txTemplate.execute {
            val payment = paymentRepository.findById(paymentId = result.requestPaymentId)!!
            payment.approve(
                paymentProvider = paymentProvider.providerName,
                providerPaymentId = result.providerPaymentId
            )

            val payload = PaymentCreationSuccessPayload(
                paymentProvider = paymentProvider.providerName
            )

            val outboxRecord = OutboxRecord.create(
                orderId = command.orderId,
                paymentId = result.requestPaymentId,
                idempotencyKey = command.eventId,
                eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
                payload = objectMapper.writeValueAsString(payload)
            )

            outboxRepository.save(outboxRecord)
        }
    }

    private fun savePaymentWithIdempotencyCheck(command: PaymentCreateCommand): PaymentSaveResult {
        return try {
            val newPayment = Payment.create(
                orderId = command.orderId,
                idempotencyKey = command.eventId,
                money = Money(amount = command.amount, currency = command.currency)
            )
            val savedPaymentId = paymentRepository.save(newPayment)
            PaymentSaveResult(paymentId = savedPaymentId, isNewlyCreated = true)
        } catch (ex: DataIntegrityViolationException) {
            val existingPaymentId =
                paymentRepository.findPaymentIdForIdempotencyKey(command.orderId, command.eventId)
                    ?: throw ex
            PaymentSaveResult(paymentId = existingPaymentId, isNewlyCreated = false)
        }
    }

    data class PaymentSaveResult(val paymentId: UUID, val isNewlyCreated: Boolean)
}


data class PaymentCreateCommand(
    val orderId: UUID,
    val eventId: UUID,
    val amount: Long,
    val currency: String
) {
    companion object {
        fun from(inboundEvent: PaymentInboundEvent): PaymentCreateCommand {
            require(inboundEvent.eventType == InboundEventType.PAYMENT_CREATE)
            require(inboundEvent.payload is PaymentCreatePayload)

            val payload = inboundEvent.payload
            return PaymentCreateCommand(
                orderId = inboundEvent.orderId,
                eventId = inboundEvent.eventId,
                amount = payload.amount,
                currency = payload.currency
            )
        }
    }
}

data class PaymentCreateResult(val paymentId: UUID)
