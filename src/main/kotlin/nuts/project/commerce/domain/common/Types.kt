package nuts.project.commerce.domain.common

enum class OrderStatus { CREATED, PAYMENT_REQUESTED, PAID, PAYMENT_FAILED }
enum class PaymentStatus { READY, PROCESSING, SUCCEEDED, FAILED }
enum class IdemStatus { IN_PROGRESS, SUCCEEDED, FAILED }
enum class StockPolicyType { RESERVE_ON_ORDER, DEDUCT_ON_PAYMENT_SUCCESS, UNLIMITED }
enum class PaymentJobStatus { PENDING, IN_PROGRESS, SUCCEEDED, FAILED }