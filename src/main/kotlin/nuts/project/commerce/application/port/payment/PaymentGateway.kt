package nuts.project.commerce.application.port.payment


interface PaymentGateway {
    /** 결제 시작(클라이언트가 결제 진행할 수 있게 세션/URL/파라미터 생성) */
    fun createPaymentSession(
        request: CreatePaymentSessionRequest
    ): CreatePaymentSessionResponse

    /** 결제 확정(승인/캡처). 멱등성 키 필수 */
    fun confirmPayment(
        request: ConfirmPaymentRequest,
        idempotencyKey: String
    ): ConfirmPaymentResponse

    /** 결제 취소/환불. 승인 전 취소(void)와 승인 후 환불(refund)을 내부에서 적절히 매핑해도 됨 */
    fun cancelPayment(
        request: CancelPaymentRequest,
        idempotencyKey: String
    ): CancelPaymentResponse

    /** 부분 환불을 강하게 표현하고 싶으면 cancel과 분리 */
    fun refundPayment(
        request: RefundPaymentRequest,
        idempotencyKey: String
    ): RefundPaymentResponse

    /** 운영/정합성 보정용 조회(웹훅 누락, 타임아웃, CS 대응에 필요) */
    fun getPayment(
        request: GetPaymentRequest
    ): PaymentSnapshot
}