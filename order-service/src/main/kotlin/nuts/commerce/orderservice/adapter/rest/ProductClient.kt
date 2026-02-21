package nuts.commerce.orderservice.adapter.rest

import nuts.commerce.orderservice.port.rest.ProductPriceResponse
import nuts.commerce.orderservice.port.rest.ProductPriceSnapshot
import nuts.commerce.orderservice.port.rest.ProductRestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.UUID

@Component
class ProductClient(
    private val restTemplate: RestTemplate,
    @Value($$"${rest.product-service.product-snapshot-endpoint}") private val snapshotUrl: String,
    @Value($$"${rest.product-service.api-key}") private val apiKey: String
) : ProductRestClient {

    override fun getPriceSnapshots(productIds: List<UUID>): ProductPriceResponse {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-API-KEY", apiKey)
        }
        val entity = HttpEntity<Void>(headers)
        val exchange = restTemplate.exchange(snapshotUrl, HttpMethod.GET, entity, String::class.java)

    }
}


data class ProductPriceResponse(
    val productPriceSnapshot: List<ProductPriceSnapshot>,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)