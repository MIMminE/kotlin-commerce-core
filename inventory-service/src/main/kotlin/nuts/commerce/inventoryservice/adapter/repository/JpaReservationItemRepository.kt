package nuts.commerce.inventoryservice.adapter.repository

import nuts.commerce.inventoryservice.port.repository.ReservationItemRepository
import nuts.commerce.inventoryservice.model.ReservationItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID


@Repository
class JpaReservationItemRepository(private val repo: ReservationItemJpa) : ReservationItemRepository {
    override fun save(items: List<ReservationItem>): List<ReservationItem> = repo.saveAll(items)

    override fun findByReservationId(reservationId: UUID): List<ReservationItem> =
        repo.findAllByReservationReservationId(reservationId)
}

interface ReservationItemJpa : JpaRepository<ReservationItem, UUID> {
    fun findAllByReservationReservationId(reservationId: UUID): List<ReservationItem>
}