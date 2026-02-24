package nuts.commerce.inventoryservice.testutil

import nuts.commerce.inventoryservice.model.Reservation
import nuts.commerce.inventoryservice.port.repository.ReservationInfo
import nuts.commerce.inventoryservice.port.repository.ReservationItemInfo
import nuts.commerce.inventoryservice.port.repository.ReservationRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryReservationRepository : ReservationRepository {
    private val store = ConcurrentHashMap<UUID, Reservation>()
    private val idempotencyKeyIndex = ConcurrentHashMap<Pair<UUID, UUID>, UUID>() // (orderId, idempotencyKey) -> reservationId

    override fun save(reservation: Reservation): Reservation {
        store[reservation.reservationId] = reservation
        idempotencyKeyIndex[Pair(reservation.orderId, reservation.idempotencyKey)] = reservation.reservationId
        return reservation
    }

    override fun findById(reservationId: UUID): Reservation? {
        return store[reservationId]
    }

    override fun findReservationIdForIdempotencyKey(orderId: UUID, idempotencyKey: UUID): ReservationInfo? {
        val reservationId = idempotencyKeyIndex[Pair(orderId, idempotencyKey)] ?: return null
        val reservation = store[reservationId] ?: return null

        return ReservationInfo(
            reservationId = reservation.reservationId,
            reservationItemInfos = reservation.items.map {
                ReservationItemInfo(
                    productId = it.productId,
                    quantity = it.qty
                )
            }
        )
    }

    override fun findReservationInfo(reservationId: UUID): ReservationInfo? {
        val reservation = store[reservationId] ?: return null

        return ReservationInfo(
            reservationId = reservation.reservationId,
            reservationItemInfos = reservation.items.map {
                ReservationItemInfo(
                    productId = it.productId,
                    quantity = it.qty
                )
            }
        )
    }

    fun clear() {
        store.clear()
        idempotencyKeyIndex.clear()
    }

    fun getAll(): List<Reservation> {
        return store.values.toList()
    }
}

