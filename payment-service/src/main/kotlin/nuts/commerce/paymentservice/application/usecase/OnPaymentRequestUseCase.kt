package nuts.commerce.paymentservice.application.usecase

import nuts.commerce.paymentservice.application.port.payment.PaymentProvider
import nuts.commerce.paymentservice.application.port.repository.PaymentRepository
import nuts.commerce.paymentservice.model.domain.Money
import nuts.commerce.paymentservice.model.domain.Payment
import java.time.Instant
import java.util.*

class OnPaymentRequestUseCase(
    private val paymentProvider: PaymentProvider,
    private val paymentRepository: PaymentRepository
) {

    data class Command(
        val orderId: UUID,
        val amount: Long,
        val currency: String,
        val paymentMethod: String,
        val idempotencyKey: UUID
    )

    data class Result(
        val paymentId: UUID,
        val status: String
    )

    fun handle(cmd: Command): Result {
        val now = Instant.now()

        // idempotency: 이미 동일한 order에 결제가 존재하면 해당 상태를 반환
        val existing = paymentRepository.findByOrderId(cmd.orderId)
        if (existing != null) {
            return Result(paymentId = existing.paymentId(), status = existing.status().name)
        }

        // create payment entity
        val payment = Payment.create(
            orderId = cmd.orderId,
            money = Money(amount = cmd.amount, currency = cmd.currency),
            idempotencyKey = cmd.idempotencyKey
        )

        // save created
        paymentRepository.save(payment)

        // start processing
        payment.startProcessing(now)
        paymentRepository.save(payment)

        // call external provider
        val chargeReq = PaymentProvider.ChargeRequest(
            orderId = cmd.orderId.toString(),
            amount = cmd.amount,
            currency = cmd.currency,
            paymentMethod = cmd.paymentMethod
        )

        val resp = try {
            paymentProvider.charge(chargeReq)
        } catch (ex: Exception) {
            // provider error -> decline
            payment.decline(now, ex.message)
            paymentRepository.save(payment)
            return Result(paymentId = payment.paymentId(), status = "FAILED")
        }

        if (resp.success) {
            payment.approve(now, resp.providerPaymentId)
            paymentRepository.save(payment)
            return Result(paymentId = payment.paymentId(), status = "APPROVED")
        } else {
            payment.decline(now, resp.failureReason)
            paymentRepository.save(payment)
            return Result(paymentId = payment.paymentId(), status = "DECLINED")
        }
    }
}