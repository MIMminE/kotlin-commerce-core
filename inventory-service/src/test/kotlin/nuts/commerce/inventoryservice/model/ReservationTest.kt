//@file:Suppress("NonAsciiCharacters")
//
//package nuts.commerce.inventoryservice.model
//
//import java.util.UUID
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertFailsWith
//
//class ReservationTest {
//
//    @Test
//    fun `생성 시 기본 상태는 RESERVED 이어야 한다`() {
//        val reservation = Reservation.create(
//            orderId = UUID.randomUUID(),
//            idempotencyKey = UUID.randomUUID()
//        )
//
//        assertEquals(ReservationStatus.RESERVED, reservation.status)
//    }
//
//    @Test
//    fun `commit 호출 시 상태가 COMMITTED 로 변경된다`() {
//        val reservation = Reservation.create(
//            orderId = UUID.randomUUID(),
//            idempotencyKey = UUID.randomUUID()
//        )
//
//        reservation.commit()
//
//        assertEquals(ReservationStatus.COMMITTED, reservation.status)
//    }
//
//    @Test
//    fun `commit 이후 다시 commit 하면 예외 발생`() {
//        val reservation = Reservation.create(
//            orderId = UUID.randomUUID(),
//            idempotencyKey = UUID.randomUUID()
//        )
//
//        reservation.commit()
//        assertFailsWith<IllegalArgumentException> { reservation.commit() }
//    }
//
//    @Test
//    fun `release는 RESERVED에서 RELEASED로 전환된다`() {
//        val reservation = Reservation.create(
//            orderId = UUID.randomUUID(),
//            idempotencyKey = UUID.randomUUID()
//        )
//
//        reservation.release()
//
//        assertEquals(ReservationStatus.RELEASED, reservation.status)
//    }
//
//    @Test
//    fun `release는 COMMITTED에서 RELEASED로 전환된다`() {
//        val reservation = Reservation.create(
//            orderId = UUID.randomUUID(),
//            idempotencyKey = UUID.randomUUID()
//        )
//
//        reservation.commit()
//        reservation.release()
//
//        assertEquals(ReservationStatus.RELEASED, reservation.status)
//    }
//
//    @Test
//    fun `이미 RELEASED 상태에서 release 호출하면 예외 발생`() {
//        val reservation = Reservation.create(
//            orderId = UUID.randomUUID(),
//            idempotencyKey = UUID.randomUUID()
//        )
//
//        reservation.release()
//        assertFailsWith<IllegalArgumentException> { reservation.release() }
//    }
//}