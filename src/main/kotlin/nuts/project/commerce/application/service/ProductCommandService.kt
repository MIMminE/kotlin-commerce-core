package nuts.project.commerce.application.service

import nuts.project.commerce.application.port.repository.ProductRepository
import org.springframework.stereotype.Service

@Service
class ProductCommandService(private val productRepository: ProductRepository) {
}