package nuts.commerce.paymentservice.adapter.payment

import nuts.commerce.paymentservice.port.payment.PaymentProvider

class CustomPaymentProvider : PaymentProvider {

    override fun charge(request: PaymentProvider.ChargeRequest): PaymentProvider.ChargeResponse {
        TODO("Not yet implemented")
    }

}