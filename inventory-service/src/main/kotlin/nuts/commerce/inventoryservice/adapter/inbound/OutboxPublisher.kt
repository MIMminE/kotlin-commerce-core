package nuts.commerce.inventoryservice.adapter.inbound

import nuts.commerce.inventoryservice.usecase.OutboxClaimUseCase
import nuts.commerce.inventoryservice.usecase.OutboxPublishUseCase

class OutboxPublisher(
    private val outboxClaimUseCase: OutboxClaimUseCase,
    private val outboxUseCase: OutboxPublishUseCase
) {
    fun execute() {
        val claimOutboxIdList = outboxClaimUseCase.execute(30)
        if (claimOutboxIdList.isNotEmpty()) {
            outboxUseCase.execute(claimOutboxIdList)
        }
    }
}