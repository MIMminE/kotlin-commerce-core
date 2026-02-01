package nuts.project.commerce.domain.core

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import nuts.project.commerce.domain.common.PaymentStatus


@Entity
@Table(name = "payments")
class Payment(
    @Id
    var id: String = "",

    var orderId: String = "",

    @Enumerated(EnumType.STRING)
    var status: PaymentStatus = PaymentStatus.READY,

    var externalTxId: String? = null,

    @Version
    var version: Long? = null
) {
    fun markProcessing() {
        if (status == PaymentStatus.SUCCEEDED || status == PaymentStatus.FAILED) return
        status = PaymentStatus.PROCESSING
    }

    fun markSucceeded(externalTxId: String) {
        status = PaymentStatus.SUCCEEDED
        this.externalTxId = externalTxId
    }

    fun markFailed() {
        status = PaymentStatus.FAILED
    }

    companion object {
        fun ready(id: String, orderId: String): Payment =
            Payment(id = id, orderId = orderId, status = PaymentStatus.READY)
    }
}