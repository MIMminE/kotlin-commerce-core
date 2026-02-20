package nuts.commerce.productservice.adapter.security.provider

import nuts.commerce.productservice.adapter.security.jwt.JwtVerifier
import nuts.commerce.productservice.adapter.security.token.JwtAuthenticationToken
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationProvider(
    private val jwtVerifier: JwtVerifier
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {
        val token = authentication as JwtAuthenticationToken

        val claims = try {
            jwtVerifier.verify(token.credentials)
        } catch (e: Exception) {
            throw BadCredentialsException("Invalid JWT", e)
        }

        val authorities = claims.roles
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { role ->
                val normalized = if (role.startsWith("ROLE_")) role else "ROLE_$role"
                SimpleGrantedAuthority(normalized)
            }

        return JwtAuthenticationToken.authenticated(
            rawToken = token.credentials,
            principal = claims.subject,
            authorities = authorities
        )
    }


    override fun supports(authentication: Class<*>): Boolean {
        return JwtAuthenticationToken::class.java.isAssignableFrom(authentication)
    }
}