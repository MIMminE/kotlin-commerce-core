package nuts.commerce.inventoryservice.port.repository

import java.util.UUID

interface OutboxClaimRepository {
    fun claimOutboxRecords(limit: Int): List<UUID>
}