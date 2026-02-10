package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.model.EventType.*
import nuts.commerce.inventoryservice.port.message.InventoryEventProducer
import nuts.commerce.inventoryservice.port.message.InventoryEvent
import nuts.commerce.inventoryservice.port.message.ProduceResult
import nuts.commerce.inventoryservice.port.message.ReservationCommittedEvent
import nuts.commerce.inventoryservice.port.message.ReservationCreationEvent
import nuts.commerce.inventoryservice.port.message.ReservationReleasedEvent
import nuts.commerce.inventoryservice.port.repository.ClaimOutboxResult
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import java.util.concurrent.Executor

@Component
class OutboxPublishUseCase(
    private val outboxRepository: OutboxRepository,
    private val eventProducer: InventoryEventProducer,
    private val objectMapper: ObjectMapper,
    @Qualifier("outboxUpdateExecutor")
    private val outboxUpdateExecutor: Executor,
    @Value($$"${inventory.outbox.batch-size:50}")
    private val batchSize: Int

) {
    fun execute() {
        val claimedOutboxResults = outboxRepository.claimAndLockBatchIds(
            batchSize = batchSize,
            lockedBy = "Nuts-Worker"
        )

        if (claimedOutboxResults.size == 0) {
            return
        }

        claimedOutboxResults.claimOutboxInfo.forEach { record ->
            val inventoryEvent = createInventoryEvent(record)

            eventProducer.produce(inventoryEvent)
                .whenCompleteAsync(
                    { result, ex ->
                        when {
                            ex != null -> {
                                outboxRepository.markFailed(
                                    outboxId = record.outboxId,
                                    lockedBy = "Nuts-Worker"
                                )
                            }

                            result is ProduceResult.Success -> {
                                outboxRepository.markPublished(
                                    outboxId = record.outboxId,
                                    lockedBy = "Nuts-Worker"
                                )
                            }

                            result is ProduceResult.Failure -> {
                                outboxRepository.markFailed(
                                    outboxId = record.outboxId,
                                    lockedBy = "Nuts-Worker"
                                )
                            }
                        }
                    }, outboxUpdateExecutor
                )
        }
    }

    private fun createInventoryEvent(record: ClaimOutboxResult.ClaimOutboxInfo): InventoryEvent {
        return when (record.eventType) {
            RESERVATION_CREATION -> {
                ReservationCreationEvent(
                    outboxId = record.outboxId,
                    orderId = record.orderId,
                    reservationId = record.reservationId,
                    createdReservationItems = objectMapper.readTree(record.payload).get("reservationInfo").map {
                        ReservationCreationEvent.CreatedReservationItem(
                            inventoryId = UUID.fromString(it.get("inventoryId").toString()),
                            quantity = it.get("quantity").asLong()
                        )
                    }
                )
            }

            RESERVATION_COMMITTED -> {
                ReservationCommittedEvent(
                    outboxId = record.outboxId,
                    orderId = record.orderId,
                    reservationId = record.reservationId,
                    commitedReservationItems = objectMapper.readTree(record.payload).get("items").map {
                        ReservationCommittedEvent.CommitedReservationItem(
                            inventoryId = UUID.fromString(it.get("inventoryId").toString()),
                            quantity = it.get("quantity").asLong()
                        )
                    }
                )
            }

            RESERVATION_RELEASED -> {
                ReservationReleasedEvent(
                    outboxId = record.outboxId,
                    orderId = record.orderId,
                    reservationId = record.reservationId,
                    releasedReservationItems = objectMapper.readTree(record.payload).get("reservationInfo").map {
                        ReservationReleasedEvent.ReleasedReservationItem(
                            inventoryId = UUID.fromString(it.get("inventoryId").toString()),
                            quantity = it.get("quantity").asLong()
                        )
                    }
                )
            }

            else -> {
                throw IllegalArgumentException("Unsupported event type: ${record.eventType}")
            }
        }
    }
}