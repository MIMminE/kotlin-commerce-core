package nuts.commerce.orderservice.adapter.rest

import nuts.commerce.orderservice.port.rest.ProductPriceSnapshot
import nuts.commerce.orderservice.port.rest.ProductRestClient
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Component
class ProductClient(
    private val restTemplate: RestTemplate
) : ProductRestClient {

    override fun getPriceSnapshots(productIds: List<String>): List<ProductPriceSnapshot> {
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
}