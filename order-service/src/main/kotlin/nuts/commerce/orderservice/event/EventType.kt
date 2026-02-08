package nuts.commerce.orderservice.event

enum class EventType {
    RESERVE_INVENTORY_REQUEST,   // 재고 예약 요청
    RESERVE_INVENTORY_CONFIRM,   // 재고 예약 확정
    RESERVE_INVENTORY_RELEASE,   // 재고 예약 반환
    PAYMENT_REQUEST,             // 결제 요청
    PAYMENT_COMPLETED,           // 결제 완료
}