package nuts.commerce.productservice.adapter.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtConfiguration(
    val secret: String = "default-secret",
    val expiration: Long = 3600000,
    val issuer: String = "product-service"
)