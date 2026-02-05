package nuts.commerce.productservice.model.domain

import nuts.commerce.productservice.model.exception.ProductException
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProductTest {

    @Test
    fun `create 정상 케이스`() {
        val id = UUID.randomUUID()
        val price = Money(1000L, "KRW")
        val p = Product.create(productId = id, productName = "name", price = price, status = Product.ProductStatus.INACTIVE)

        assertEquals(id, p.productId)
        assertEquals("name", p.productName)
        assertEquals(1000L, p.price.amount)
        assertEquals("KRW", p.price.currency)
        assertEquals(Product.ProductStatus.INACTIVE, p.status)
    }

    @Test
    fun `activate - INACTIVE에서 ACTIVE로 전이`() {
        val p = Product.create(productName = "n", price = Money(1L, "KRW"), status = Product.ProductStatus.INACTIVE)

        p.activate()

        assertEquals(Product.ProductStatus.ACTIVE, p.status)
    }

    @Test
    fun `activate - ACTIVE에서 호출하면 InvalidTransition 예외`() {
        val p = Product.create(productName = "n", price = Money(1L, "KRW"), status = Product.ProductStatus.ACTIVE)

        assertFailsWith<ProductException.InvalidTransition> {
            p.activate()
        }
    }

    @Test
    fun `deactivate - ACTIVE에서 INACTIVE로 전이`() {
        val p = Product.create(productName = "n", price = Money(1L, "KRW"), status = Product.ProductStatus.ACTIVE)

        p.deactivate()

        assertEquals(Product.ProductStatus.INACTIVE, p.status)
    }

    @Test
    fun `deactivate - INACTIVE에서 호출하면 InvalidTransition 예외`() {
        val p = Product.create(productName = "n", price = Money(1L, "KRW"), status = Product.ProductStatus.INACTIVE)

        assertFailsWith<ProductException.InvalidTransition> {
            p.deactivate()
        }
    }

    @Test
    fun `delete - 어떤 상태에서도 삭제 가능하되 이미 DELETED이면 예외`() {
        val p = Product.create(productName = "n", price = Money(1L, "KRW"), status = Product.ProductStatus.ACTIVE)

        p.delete()
        assertEquals(Product.ProductStatus.DELETED, p.status)

        // 이미 DELETED 상태에서 다시 delete 호출 시 예외
        assertFailsWith<ProductException.InvalidTransition> {
            p.delete()
        }
    }

    @Test
    fun `updatePrice - 정상 케이스`() {
        val p = Product.create(productName = "n", price = Money(100L, "KRW"))

        p.updatePrice(Money(200L, "KRW"))
        assertEquals(200L, p.price.amount)
    }

    @Test
    fun `updatePrice - 음수이면 InvalidCommand 예외`() {
        val p = Product.create(productName = "n", price = Money(100L, "KRW"))

        assertFailsWith<ProductException.InvalidCommand> {
            p.updatePrice(Money(-1L, "KRW"))
        }
    }

    @Test
    fun `updateName - 정상 케이스`() {
        val p = Product.create(productName = "old", price = Money(100L, "KRW"))

        p.updateName("new")
        assertEquals("new", p.productName)
    }

    @Test
    fun `updateName - blank이면 InvalidCommand 예외`() {
        val p = Product.create(productName = "old", price = Money(100L, "KRW"))

        assertFailsWith<ProductException.InvalidCommand> {
            p.updateName("")
        }
    }
}
