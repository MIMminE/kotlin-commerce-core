package nuts.project.commerce.application.service

import nuts.project.commerce.application.port.payment.ConfirmPaymentRequest
import nuts.project.commerce.application.port.payment.CreatePaymentSessionRequest
import nuts.project.commerce.application.port.payment.PaymentGateway
import nuts.project.commerce.application.port.repository.PaymentRepository
import nuts.project.commerce.domain.core.Payment
import nuts.project.commerce.domain.core.payment.PaymentStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

@Service
class PaymentService(
    private val paymentGateway: PaymentGateway,
    private val paymentRepository: PaymentRepository,
    private val txTemplate: TransactionTemplate
) {

    fun createPaymentSession(orderId: UUID, amount: Money): UUID {

        val payment = txTemplate.execute {
            paymentRepository.save(Payment.create(orderId, amount))
        }

        val paymentResponse =
            paymentGateway.createPaymentSession(CreatePaymentSessionRequest(orderId, amount))

        txTemplate.execute {
            paymentRepository.update(paymentResponse.paymentId, paymentResponse.pgProvider, paymentResponse.pgSessionId)
        }

        return paymentResponse.paymentId
    }

    fun confirmPayment(paymentId: UUID) {
        // 외부 결제 모듈과 통신을 거쳐서 성공하면 디비로 상태 업데이트 진행 , 현재는 동기식 코드로 작성되었지만 추후에 변경 가능
        paymentRepository.findByIdAndStatus(paymentId, PaymentStatus.INITIATED)?.let {
            paymentGateway.confirmPayment(ConfirmPaymentRequest(paymentId))
            txTemplate.execute {
                paymentRepository.updateStatus(
                    paymentId = paymentId,
                    status = PaymentStatus.APPROVED,
                )
            }
        }
    }
}