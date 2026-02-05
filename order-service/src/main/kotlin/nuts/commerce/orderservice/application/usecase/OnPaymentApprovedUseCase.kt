package nuts.commerce.orderservice.application.usecase

import jakarta.transaction.Transactional
import nuts.commerce.orderservice.application.port.repository.OrderRepository
import nuts.commerce.orderservice.application.port.repository.PaymentResultRecordRepository
import nuts.commerce.orderservice.model.domain.Order
import nuts.commerce.orderservice.model.exception.OrderException
import nuts.commerce.orderservice.model.infra.PaymentResultRecord
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OnPaymentApprovedUseCase(
    private val orderRepository: OrderRepository,
    private val paymentResultRecordRepository: PaymentResultRecordRepository
) {

    @Transactional
    fun handle(event: PaymentApprovedEvent) {
        val getOrCreateResult = paymentResultRecordRepository.getOrCreate(
            PaymentResultRecord.receive(
                eventId = event.eventId,
                orderId = event.orderId,
                eventType = PaymentResultRecord.EventType.PAYMENT_SUCCESS,
                payload = event.payload
            )
        )

        if (!getOrCreateResult.isCreated) return

        val record = getOrCreateResult.record
        val order = orderRepository.findById(event.orderId)
            ?: run {
                record.markFailed("order not found")
                paymentResultRecordRepository.save(record)
                return
            }

        if (order.status == Order.OrderStatus.PAID) {
            record.markFailed("order already PAID")
            paymentResultRecordRepository.save(record)
            return
        }

        try {
            order.applyPaymentApproved()
        } catch (e: OrderException.InvalidTransition) {
            record.markFailed("invalid transition: ${e.message}")
            paymentResultRecordRepository.save(record)
            throw e
        }

        orderRepository.save(order)
        record.markProcessed()
        paymentResultRecordRepository.save(record)
    }

    data class PaymentApprovedEvent(
        val eventId: UUID,
        val orderId: UUID,
        val paymentId: UUID,
        val payload: String,
    )
}