package nuts.commerce.productservice.adapter.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.security.Key
import java.util.Date
import javax.crypto.spec.SecretKeySpec

@Component
@EnableConfigurationProperties(JwtConfiguration::class)
class JwtVerifier(private val jwtConfiguration: JwtConfiguration) {
    private val key: Key = SecretKeySpec(jwtConfiguration.secret.toByteArray(), SignatureAlgorithm.HS256.jcaName)

    fun verify(rawToken: String): JwtClaims {
        val claims: Claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(rawToken)
            .body

        val exp = claims.expiration ?: throw IllegalArgumentException("Missing exp")
        if (exp.before(Date())) throw IllegalArgumentException("Token expired")

        val subject = claims.subject ?: throw IllegalArgumentException("Missing sub")

        val roles = (claims["roles"] as? Collection<*>)?.mapNotNull { it?.toString() } ?: emptyList()

        return JwtClaims(
            subject = subject,
            roles = roles,
            expiresAt = exp
        )
    }
}

data class JwtClaims(
    val subject: String,
    val roles: List<String>,
    val expiresAt: Date
)