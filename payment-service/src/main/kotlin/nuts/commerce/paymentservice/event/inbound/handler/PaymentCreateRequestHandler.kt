package nuts.commerce.paymentservice.event.inbound.handler

import nuts.commerce.paymentservice.event.inbound.InboundEventType
import nuts.commerce.paymentservice.event.inbound.PaymentCreatePayload
import nuts.commerce.paymentservice.event.inbound.PaymentInboundEvent
import nuts.commerce.paymentservice.event.outbound.OutboundEventType
import nuts.commerce.paymentservice.event.outbound.PaymentCreationFailPayload
import nuts.commerce.paymentservice.event.outbound.PaymentCreationSuccessPayload
import nuts.commerce.paymentservice.model.Money
import nuts.commerce.paymentservice.model.OutboxRecord
import nuts.commerce.paymentservice.model.Payment
import nuts.commerce.paymentservice.port.payment.CreatePaymentRequest
import nuts.commerce.paymentservice.port.payment.CreatePaymentResult
import nuts.commerce.paymentservice.port.payment.PaymentProvider
import nuts.commerce.paymentservice.port.repository.OutboxRepository
import nuts.commerce.paymentservice.port.repository.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Component
class PaymentCreateRequestHandler(
    private val paymentRepository: PaymentRepository,
    private val outboxRepository: OutboxRepository,
    private val paymentProvider: PaymentProvider,
    private val objectMapper: ObjectMapper,
    private val txTemplate: TransactionTemplate
) : PaymentEventHandler {

    private val logger = LoggerFactory.getLogger(PaymentCreateRequestHandler::class.java)

    override val supportType: InboundEventType
        get() = InboundEventType.PAYMENT_CREATE_REQUEST

    override fun handle(paymentInboundEvent: PaymentInboundEvent) {
        val eventId = paymentInboundEvent.eventId
        val orderId = paymentInboundEvent.orderId
        val payload = paymentInboundEvent.payload as PaymentCreatePayload

        val paymentSaveResult = savePaymentWithIdempotencyCheck(
            orderId = orderId,
            eventId = eventId,
            amount = payload.amount,
            currency = payload.currency
        )

        if (!paymentSaveResult.isNewlyCreated) {
            logger.info("Payment already exists for orderId: $orderId, eventId: $eventId. Skipping processing.")
            return
        }

        val createPaymentRequest = CreatePaymentRequest(
            orderId = orderId,
            paymentId = paymentSaveResult.paymentId,
            amount = payload.amount,
            currency = payload.currency
        )

        paymentProvider.createPayment(createPaymentRequest)
            .whenComplete { result, _ ->
                when (result) {
                    is CreatePaymentResult.Success -> successHandle(result, orderId, eventId)
                    is CreatePaymentResult.Failure -> failureHandle(
                        result,
                        orderId,
                        eventId,
                        paymentSaveResult.paymentId
                    )
                }
            }
    }

    private fun savePaymentWithIdempotencyCheck(
        orderId: UUID,
        eventId: UUID,
        amount: Long,
        currency: String
    ): PaymentSaveResult {
        return try {
            val newPayment = Payment.create(
                orderId = orderId,
                idempotencyKey = eventId,
                money = Money(amount = amount, currency = currency)
            )
            val savedPaymentId = paymentRepository.save(newPayment)
            PaymentSaveResult(paymentId = savedPaymentId, isNewlyCreated = true)
        } catch (ex: DataIntegrityViolationException) {
            val existingPaymentId =
                paymentRepository.findPaymentIdForIdempotencyKey(orderId, eventId)
                    ?: throw ex
            PaymentSaveResult(paymentId = existingPaymentId, isNewlyCreated = false)
        }
    }

    data class PaymentSaveResult(val paymentId: UUID, val isNewlyCreated: Boolean)

    private fun successHandle(
        result: CreatePaymentResult.Success,
        orderId: UUID,
        eventId: UUID
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
                orderId = orderId,
                paymentId = result.requestPaymentId,
                idempotencyKey = eventId,
                eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
                payload = objectMapper.writeValueAsString(payload)
            )

            outboxRepository.save(outboxRecord)
        }
    }

    private fun failureHandle(result: CreatePaymentResult.Failure, eventId: UUID, orderId: UUID, paymentId: UUID) {
        txTemplate.execute {

            paymentRepository.findById(paymentId)
                ?.fail(result.reason)

            PaymentCreationFailPayload(result.reason).let { payload ->
                val outboxRecord = OutboxRecord.create(
                    orderId = orderId,
                    paymentId = paymentId,
                    idempotencyKey = eventId,
                    eventType = OutboundEventType.PAYMENT_CREATION_FAILED,
                    payload = objectMapper.writeValueAsString(payload)
                )
                outboxRepository.save(outboxRecord)
            }
        }
    }
}