package nuts.commerce.productservice.adapter.security.jwt

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.security.Key
import java.util.Date
import javax.crypto.spec.SecretKeySpec

@Component
@EnableConfigurationProperties(JwtConfiguration::class)
class JwtIssuer(private val jwtConfiguration: JwtConfiguration) {
    private val key: Key = SecretKeySpec(jwtConfiguration.secret.toByteArray(), SignatureAlgorithm.HS256.jcaName)

    fun issue(
        subject: String,
        roles: List<String> = emptyList(),
        extraClaims: Map<String, Any> = emptyMap()
    ): String {
        val now = Date()
        val exp = Date(now.time + jwtConfiguration.expiration)

        val builder = Jwts.builder()
            .setSubject(subject)
            .setIssuer(jwtConfiguration.issuer)
            .setIssuedAt(now)
            .setExpiration(exp)
            .claim("roles", roles)
            .signWith(key, SignatureAlgorithm.HS256)

        extraClaims.forEach { (k, v) -> builder.claim(k, v) }

        return builder.compact()
    }
}