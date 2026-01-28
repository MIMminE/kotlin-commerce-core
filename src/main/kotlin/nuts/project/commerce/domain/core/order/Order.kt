package nuts.project.commerce.domain.core.order

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import nuts.project.commerce.domain.common.BaseEntity
import java.util.UUID

@Entity
@Table(name = "orders")
class Order protected constructor() : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID()

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    lateinit var userId: UUID
        protected set


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.CREATED
        protected set


    @Column(name = "original_amount", nullable = false)
    var originalAmount: Long = 0
        protected set

    @Column(name = "discount_amount", nullable = false)
    var discountAmount: Long = 0
        protected set

    @Column(name = "final_amount", nullable = false)
    var finalAmount: Long = 0
        protected set

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _items: MutableList<OrderItem> = mutableListOf()

    val items: List<OrderItem> get() = _items.toList()


    @Column(name = "applied_coupon_id", columnDefinition = "uuid")
    var appliedCouponId: UUID? = null
        protected set

    companion object {
        fun create(userId: UUID): Order {
            return Order().apply {
                this.userId = userId
                recalculateAmounts(discountAmount = 0)
            }
        }
    }

    fun addItem(productId: UUID, qty: Long, unitPriceSnapshot: Long) {
        require(status == OrderStatus.CREATED) { "cannot modify items unless CREATED" }
        val item = OrderItem.of(productId, qty, unitPriceSnapshot)
        item.attachTo(this)
        _items.add(item)
        recalculateAmounts(discountAmount)
    }

    fun applyDiscount(discountAmount: Long, couponId: UUID?) {
        require(status == OrderStatus.CREATED) { "cannot apply discount unless CREATED" }
        require(discountAmount >= 0) { "discountAmount must be >= 0" }
        this.appliedCouponId = couponId
        recalculateAmounts(discountAmount)
    }

    fun markPaid() {
        require(status == OrderStatus.CREATED) { "only CREATED order can be PAID" }
        require(finalAmount >= 0) { "finalAmount must be >= 0" }
        this.status = OrderStatus.PAID
    }

    fun markFailed() {
        require(status == OrderStatus.CREATED) { "only CREATED order can be FAILED" }
        this.status = OrderStatus.FAILED
    }

    private fun recalculateAmounts(discountAmount: Long) {
        val sum = _items.sumOf { it.unitPriceSnapshot * it.qty }
        require(sum >= 0) { "originalAmount must be >= 0" }

        this.originalAmount = sum
        this.discountAmount = minOf(discountAmount, sum)
        this.finalAmount = this.originalAmount - this.discountAmount
    }
}