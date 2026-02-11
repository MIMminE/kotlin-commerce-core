package nuts.commerce.paymentservice.adapter.payment

import nuts.commerce.paymentservice.port.payment.ChargeRequest
import nuts.commerce.paymentservice.port.payment.ChargeResult
import nuts.commerce.paymentservice.port.payment.PaymentProvider
import nuts.commerce.paymentservice.port.payment.PaymentStatusUpdateResult
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryPaymentProvider : PaymentProvider{

    override val providerName: String
        get() = "in-memory"

    private enum class State { CHARGED, COMMITTED, RELEASED }

    private data class PaymentRecord(
        var state: State,
        val processedEventIds: MutableSet<UUID>
    )

    // providerPaymentId(프로바이더 결제 ID) -> PaymentRecord(결제 레코드 매핑)
    private val records = ConcurrentHashMap<UUID, PaymentRecord>()

    override fun charge(request: ChargeRequest): CompletableFuture<ChargeResult> {
        val providerPaymentId = UUID.randomUUID()
        // 이 providerPaymentId에 대한 새로운 레코드 생성
        val record = PaymentRecord(state = State.CHARGED, processedEventIds = ConcurrentHashMap.newKeySet())
        records[providerPaymentId] = record

        val result = ChargeResult.Success(providerPaymentId = providerPaymentId, requestPaymentId = request.paymentId)
        return CompletableFuture.completedFuture(result)
    }

    override fun commitPayment(
        providerPaymentId: UUID,
        eventId: UUID
    ): CompletableFuture<PaymentStatusUpdateResult> {
        val record = records[providerPaymentId]
            ?: return CompletableFuture.completedFuture(
                PaymentStatusUpdateResult.Failure("providerPaymentId not found", providerPaymentId)
            )

        synchronized(record) {
            // eventId(이벤트 ID) 기준 멱등성 처리
            if (record.processedEventIds.contains(eventId)) {
                return CompletableFuture.completedFuture(PaymentStatusUpdateResult.Success(providerPaymentId))
            }

            when (record.state) {
                State.COMMITTED -> {
                    record.processedEventIds.add(eventId)
                    return CompletableFuture.completedFuture(PaymentStatusUpdateResult.Success(providerPaymentId))
                }
                State.RELEASED -> {
                    // 이미 release(환불/취소)된 경우 commit 불가
                    record.processedEventIds.add(eventId)
                    return CompletableFuture.completedFuture(
                        PaymentStatusUpdateResult.Failure("already released", providerPaymentId)
                    )
                }
                State.CHARGED -> {
                    record.state = State.COMMITTED
                    record.processedEventIds.add(eventId)
                    return CompletableFuture.completedFuture(PaymentStatusUpdateResult.Success(providerPaymentId))
                }
            }
        }
        // 도달되지 않는 코드(안전성을 위해 기본 실패 반환)
        return CompletableFuture.completedFuture(PaymentStatusUpdateResult.Failure("unknown error", providerPaymentId))
    }

    override fun releasePayment(
        providerPaymentId: UUID,
        eventId: UUID
    ): CompletableFuture<PaymentStatusUpdateResult> {
        val record = records[providerPaymentId]
            ?: return CompletableFuture.completedFuture(
                PaymentStatusUpdateResult.Failure("providerPaymentId not found", providerPaymentId)
            )

        synchronized(record) {
            // eventId(이벤트 ID) 기준 멱등성 처리
            if (record.processedEventIds.contains(eventId)) {
                return CompletableFuture.completedFuture(PaymentStatusUpdateResult.Success(providerPaymentId))
            }

            when (record.state) {
                State.RELEASED -> {
                    record.processedEventIds.add(eventId)
                    return CompletableFuture.completedFuture(PaymentStatusUpdateResult.Success(providerPaymentId))
                }
                State.COMMITTED -> {
                    // 이미 commit된 경우 release 불가
                    record.processedEventIds.add(eventId)
                    return CompletableFuture.completedFuture(
                        PaymentStatusUpdateResult.Failure("already committed", providerPaymentId)
                    )
                }
                State.CHARGED -> {
                    record.state = State.RELEASED
                    record.processedEventIds.add(eventId)
                    return CompletableFuture.completedFuture(PaymentStatusUpdateResult.Success(providerPaymentId))
                }
            }
        }
        // 도달되지 않는 코드(안전성을 위해 기본 실패 반환)
    }
}