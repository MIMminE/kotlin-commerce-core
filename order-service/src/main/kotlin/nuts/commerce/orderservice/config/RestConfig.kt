package nuts.commerce.orderservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriBuilderFactory

@Configuration
class RestConfig {
    @Bean
    fun restTemplate(
        @Value($$"${order-service.product.base-url:http://localhost:8081}") baseUrl: String
    ): RestTemplate {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(2000)
            setReadTimeout(5000)
        }
        val rest = RestTemplate(factory)
        rest.uriTemplateHandler = DefaultUriBuilderFactory(baseUrl)
        return rest
    }
}