package nuts.project.commerce.application.service

import nuts.project.commerce.application.port.repository.ProductRepository
import nuts.project.commerce.domain.core.product.Product
import org.springframework.stereotype.Service
import java.util.*

@Service
class ProductService(private val productRepository: ProductRepository) {

    fun getProducts(productIds: List<UUID>): List<Product> {
        val products = productRepository.findByIds(productIds)
        check(products.size == productIds.size) { "조회한 상품 수가 일치하지 않습니다. expected=${productIds.size}, actual=${products.size}" }
        return products
    }
}