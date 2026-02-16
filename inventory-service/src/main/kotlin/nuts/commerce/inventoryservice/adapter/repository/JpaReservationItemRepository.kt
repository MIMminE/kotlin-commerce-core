package nuts.commerce.inventoryservice.adapter.repository

import nuts.commerce.inventoryservice.model.ReservationItem
import nuts.commerce.inventoryservice.port.repository.ReservationItemRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaReservationItemRepository(private val reservationItemJpa: ReservationItemJpa) : ReservationItemRepository {
    override fun save(items: List<ReservationItem>): List<ReservationItem> {
        return reservationItemJpa.saveAllAndFlush(items)
    }

    override fun findByReservationId(reservationId: UUID): List<ReservationItem> {
        return reservationItemJpa.findAllByReservationId(reservationId)
    }
}

interface ReservationItemJpa : JpaRepository<ReservationItem, UUID> {
    fun findAllByReservationId(reservationId: UUID): List<ReservationItem>
}