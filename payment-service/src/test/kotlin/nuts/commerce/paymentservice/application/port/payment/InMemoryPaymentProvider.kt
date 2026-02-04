package nuts.commerce.paymentservice.application.port.payment

import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Simple in-memory payment provider for tests.
 * You can enqueue predefined responses (ChargeResponse) that will be returned in order.
 * If no responses are enqueued, default behaviour is to return success with a random providerPaymentId.
 */
class InMemoryPaymentProvider(
    private val defaultSuccess: Boolean = true
) : PaymentProvider {

    private val responses = ConcurrentLinkedQueue<PaymentProvider.ChargeResponse>()

    fun enqueueResponse(response: PaymentProvider.ChargeResponse) = responses.add(response)

    fun clearResponses() = responses.clear()

    override fun charge(request: PaymentProvider.ChargeRequest): PaymentProvider.ChargeResponse {
        val polled = responses.poll()
        return polled ?: if (defaultSuccess) {
            PaymentProvider.ChargeResponse(
                success = true,
                providerPaymentId = UUID.randomUUID().toString(),
                failureReason = null
            )
        } else {
            PaymentProvider.ChargeResponse(
                success = false,
                providerPaymentId = null,
                failureReason = "simulated-failure"
            )
        }
    }
}
