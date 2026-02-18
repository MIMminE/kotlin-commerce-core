package nuts.commerce.paymentservice.adapter.payment

import nuts.commerce.paymentservice.port.payment.CreatePaymentRequest
import nuts.commerce.paymentservice.port.payment.CreatePaymentResult
import nuts.commerce.paymentservice.port.payment.PaymentProvider
import nuts.commerce.paymentservice.port.payment.PaymentStatusUpdateResult
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryPaymentProvider : PaymentProvider{
    override val providerName: String
        get() = "InMemoryPaymentProvider"

    private enum class State { CHARGED, COMMITTED, RELEASED }

    private data class PaymentRecord(
        var state: State,
        val processedEventIds: MutableSet<UUID>
    )

    private val records = ConcurrentHashMap<UUID, PaymentRecord>()

    override fun createPayment(request: CreatePaymentRequest): CompletableFuture<CreatePaymentResult> {
        val providerPaymentId = UUID.randomUUID()
        val record = PaymentRecord(state = State.CHARGED, processedEventIds = ConcurrentHashMap.newKeySet())
        records[providerPaymentId] = record

        val result = CreatePaymentResult.Success(providerPaymentId = providerPaymentId, requestPaymentId = request.paymentId)
        return CompletableFuture.completedFuture(result)
    }

    override fun commitPayment(
        providerPaymentId: UUID,
        eventId: UUID
    ): CompletableFuture<PaymentStatusUpdateResult> {
        val record = records[providerPaymentId]
            ?: return CompletableFuture.failedFuture(IllegalArgumentException("providerPaymentId not found: $providerPaymentId"))

        synchronized(record) {
            // 이미 처리된 이벤트면 성공으로 응답(멱등성)
            if (record.processedEventIds.contains(eventId)) {
                return CompletableFuture.completedFuture(PaymentStatusUpdateResult.Success(providerPaymentId))
            }

            when (record.state) {
                State.CHARGED -> {
                    record.state = State.COMMITTED
                    record.processedEventIds.add(eventId)
                    return CompletableFuture.completedFuture(PaymentStatusUpdateResult.Success(providerPaymentId))
                }
                State.COMMITTED -> {
                    // 이미 커밋된 상태, 이벤트 ID 저장 후 성공
                    record.processedEventIds.add(eventId)
                    return CompletableFuture.completedFuture(PaymentStatusUpdateResult.Success(providerPaymentId))
                }
                State.RELEASED -> {
                    return CompletableFuture.failedFuture(IllegalStateException("cannot commit: already released: $providerPaymentId"))
                }
            }
        }
    }

    override fun releasePayment(
        providerPaymentId: UUID,
        eventId: UUID
    ): CompletableFuture<PaymentStatusUpdateResult> {
        val record = records[providerPaymentId]
            ?: return CompletableFuture.failedFuture(IllegalArgumentException("providerPaymentId not found: $providerPaymentId"))

        synchronized(record) {
            if (record.processedEventIds.contains(eventId)) {
                return CompletableFuture.completedFuture(PaymentStatusUpdateResult.Success(providerPaymentId))
            }

            when (record.state) {
                State.CHARGED -> {
                    record.state = State.RELEASED
                    record.processedEventIds.add(eventId)
                    return CompletableFuture.completedFuture(PaymentStatusUpdateResult.Success(providerPaymentId))
                }
                State.RELEASED -> {
                    record.processedEventIds.add(eventId)
                    return CompletableFuture.completedFuture(PaymentStatusUpdateResult.Success(providerPaymentId))
                }
                State.COMMITTED -> {
                    return CompletableFuture.failedFuture(IllegalStateException("cannot release: already committed: $providerPaymentId"))
                }
            }
        }
    }
}