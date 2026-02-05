package nuts.commerce.paymentservice.application.usecase

import nuts.commerce.paymentservice.application.port.payment.PaymentProvider
import nuts.commerce.paymentservice.application.port.repository.PaymentRepository
import nuts.commerce.paymentservice.model.domain.Money
import nuts.commerce.paymentservice.model.domain.Payment
import java.time.Instant
import java.util.*


// TODO
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

        val existing = paymentRepository.findByOrderId(cmd.orderId)
        if (existing != null) {
            return Result(paymentId = existing.paymentId, status = existing.status.name)
        }

        val payment = Payment.create(
            orderId = cmd.orderId,
            money = Money(amount = cmd.amount, currency = cmd.currency),
            idempotencyKey = cmd.idempotencyKey
        )

        paymentRepository.save(payment)

        payment.startProcessing(now)
        paymentRepository.save(payment)

        val chargeReq = PaymentProvider.ChargeRequest(
            orderId = cmd.orderId.toString(),
            amount = cmd.amount,
            currency = cmd.currency,
            paymentMethod = cmd.paymentMethod
        )

        val resp = try {
            paymentProvider.charge(chargeReq)
        } catch (ex: Exception) {
            payment.fail(now)
            paymentRepository.save(payment)
            return Result(paymentId = payment.paymentId, status = "FAILED")
        }

        if (resp.success) {
            payment.approve(now)
            paymentRepository.save(payment)
            return Result(paymentId = payment.paymentId, status = "APPROVED")
        } else {
            payment.decline(now)
            paymentRepository.save(payment)
            return Result(paymentId = payment.paymentId, status = "DECLINED")
        }
    }
}