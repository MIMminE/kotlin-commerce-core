package nuts.commerce.orderservice.domain


enum class OutboxStatus {
    PENDING,    // 아직 발행 시도 전(또는 즉시 처리 대기)
    PUBLISHED,  // 발행 완료
    FAILED,     // 마지막 시도가 실패(재시도 예정일 수 있음)
    DEAD        // 최대 재시도 초과 등으로 더 이상 처리하지 않음
}