package nuts.commerce.productservice.adapter.message

import nuts.commerce.productservice.port.message.ProduceResult
import nuts.commerce.productservice.port.message.ProductEventProducer
import nuts.commerce.productservice.port.message.ProductUpdateInfo
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@ConditionalOnProperty(
    prefix = "product.kafka.producer",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class KafkaProductEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, ProductUpdateInfo>,
    @Value($$"${product.kafka.producer}") private val productTopic: String
) : ProductEventProducer {

    override fun produce(productUpdateInfo: ProductUpdateInfo): ProduceResult {
        return try {
            kafkaTemplate.send(productTopic, productUpdateInfo).get()
            ProduceResult.Success(
                requestPrincipalName = productUpdateInfo.requestPrincipalName,
                productId = productUpdateInfo.productId
            )
        } catch (ex: Exception) {
            ProduceResult.Failure(
                requestPrincipalName = productUpdateInfo.requestPrincipalName,
                productId = productUpdateInfo.productId,
                reason = ex.message ?: "Unknown error"
            )
        }
    }
}