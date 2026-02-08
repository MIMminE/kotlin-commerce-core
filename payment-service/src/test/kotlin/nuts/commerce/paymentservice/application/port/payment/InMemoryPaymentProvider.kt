package nuts.commerce.paymentservice.application.port.payment

import nuts.commerce.paymentservice.port.payment.PaymentProvider
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

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
