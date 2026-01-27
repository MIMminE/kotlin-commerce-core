package nuts.project.commerce.application.service

import nuts.project.commerce.domain.common.Money
import java.util.UUID

class PaymentCommandService {

    fun initiate(orderId: UUID, amount: Money, successUrl: String, failUrl: String) {
        // Implementation for initiating payment
    }
}