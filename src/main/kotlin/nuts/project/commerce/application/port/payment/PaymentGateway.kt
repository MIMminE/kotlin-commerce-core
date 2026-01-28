package nuts.project.commerce.application.port.payment


interface PaymentGateway {
    fun createPaymentSession(
        request: CreatePaymentSessionRequest
    ): CreatePaymentSessionResponse

    fun confirmPayment(
        request: ConfirmPaymentRequest
    ): ConfirmPaymentResponse

    fun cancelPayment(
        request: CancelPaymentRequest
    ): CancelPaymentResponse

    fun refundPayment(
        request: RefundPaymentRequest
    ): RefundPaymentResponse

    fun getPayment(
        request: GetPaymentRequest
    ): PaymentSnapshot
}