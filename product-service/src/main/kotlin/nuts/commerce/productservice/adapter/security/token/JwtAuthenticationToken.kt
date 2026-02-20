package nuts.commerce.productservice.adapter.security.token

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class JwtAuthenticationToken private constructor(
    private val rawToken: String,
    private val principal: Any?,
    authorities: Collection<GrantedAuthority>?
) : AbstractAuthenticationToken(authorities) {

    companion object {
        fun unauthenticated(rawToken: String): JwtAuthenticationToken =
            JwtAuthenticationToken(rawToken, null, null).apply { isAuthenticated = false }

        fun authenticated(
            rawToken: String,
            principal: Any,
            authorities: Collection<GrantedAuthority>
        ): JwtAuthenticationToken =
            JwtAuthenticationToken(rawToken, principal, authorities).apply { isAuthenticated = true }
    }

    override fun getCredentials(): String {
        return rawToken
    }

    override fun getPrincipal(): Any? {
        return principal
    }
}