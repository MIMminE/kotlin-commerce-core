package nuts.commerce.orderservice.adapter.rest

import nuts.commerce.orderservice.port.rest.ProductPriceResponse
import nuts.commerce.orderservice.port.rest.ProductPriceSnapshot
import nuts.commerce.orderservice.port.rest.ProductRestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.UUID

@Component
class ProductClient(
    private val restTemplate: RestTemplate,
    @Value($$"${rest.product-snapshot.url}") private val productServiceUrl: String
) : ProductRestClient {

    override fun getPriceSnapshots(productIds: List<String>):ProductPriceResponse{
        val path = "/api/products/price-snapshots"
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val body = productIds
        val req = HttpEntity(body, headers)
        return try {
            val resp: ResponseEntity<Array<ProductPriceSnapshot>> = restTemplate.postForEntity(path, req, Array<ProductPriceSnapshot>::class.java)
            resp.body?.toList() ?: emptyList()
        } catch (ex: HttpClientErrorException) {
            throw RuntimeException("failed to fetch price snapshots", ex)
        }
    }

    override fun getPriceSnapshots(productIds: List<UUID>): ProductPriceResponse {

        TODO("Not yet implemented")
    }
}