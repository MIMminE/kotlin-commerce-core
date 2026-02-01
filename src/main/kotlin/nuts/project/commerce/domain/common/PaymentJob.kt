package nuts.project.commerce.domain.common

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "payment_jobs")
class PaymentJob(
    @Id
    var id: String = "",

    var paymentId: String = "",

    @Enumerated(EnumType.STRING)
    var status: PaymentJobStatus = PaymentJobStatus.PENDING,

    var attempts: Int = 0,
    var nextRunAt: Instant = Instant.now()
) {
    fun tryClaim(): Boolean {
        if (status != PaymentJobStatus.PENDING) return false
        status = PaymentJobStatus.IN_PROGRESS
        attempts += 1
        return true
    }

    fun markSucceeded() {
        status = PaymentJobStatus.SUCCEEDED
    }

    fun markFailed() {
        status = PaymentJobStatus.FAILED
    }

    companion object {
        fun pending(id: String, paymentId: String): PaymentJob =
            PaymentJob(id = id, paymentId = paymentId, status = PaymentJobStatus.PENDING)
    }
}