package nuts.commerce.productservice.adapter.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.security.Key
import java.security.SignatureException
import java.util.Date
import javax.crypto.spec.SecretKeySpec

@Component
@EnableConfigurationProperties(JwtConfiguration::class)
class JwtComponent(
    private val jwtConfiguration: JwtConfiguration
) {
    private val key: Key = SecretKeySpec(jwtConfiguration.secret.toByteArray(), SignatureAlgorithm.HS256.jcaName)
    private val logger: Logger = LoggerFactory.getLogger(JwtComponent::class.java)

    fun generateToken(username: String, roles: List<String>): String {
        val claims: Claims = Jwts.claims().setSubject(username)
        claims["roles"] = roles

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + jwtConfiguration.expiration))
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            val claims: Claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body

            !claims.expiration.before(Date())
        } catch (e: SignatureException) {
            logger.error("Invalid JWT signature: ${e.message}")
            false
        } catch (e: Exception) {
            logger.error("JWT validation error: ${e.message}")
            false
        }
    }

    fun getUsernameFromToken(token: String): String? {
        return try {
            val claims: Claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body

            claims.subject // 사용자 이름 반환
        } catch (e: Exception) {
            logger.error("Error extracting username from JWT: ${e.message}")
            null
        }
    }

    fun getRoleFromToken(token: String): List<String> {
        return try {
            val claims: Claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body

            claims["roles"] as? List<String> ?: emptyList()
        } catch (e: Exception) {
            logger.error("Error extracting roles from JWT: ${e.message}")
            throw RuntimeException("Invalid JWT token: ${e.message}", e)
        }
    }
}


@ConfigurationProperties(prefix = "jwt")
data class JwtConfiguration(
    val secret: String = "default-secret",
    val expiration: Long = 3600000,
    val subject: String = "product-service"
)