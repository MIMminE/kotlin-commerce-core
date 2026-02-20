package nuts.commerce.productservice.adapter.security.token

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class ApiKeyAuthenticationToken private constructor(
    val apiKey: String,
    private val principalValue: Any?,
    authorities: Collection<GrantedAuthority>?
) : AbstractAuthenticationToken(authorities) {

    companion object {
        fun unauthenticated(apiKey: String): ApiKeyAuthenticationToken =
            ApiKeyAuthenticationToken(apiKey, null, null).apply { isAuthenticated = false }

        fun authenticated(
            apiKey: String,
            principal: Any,
            authorities: Collection<GrantedAuthority>
        ): ApiKeyAuthenticationToken =
            ApiKeyAuthenticationToken(apiKey, principal, authorities).apply { isAuthenticated = true }
    }

    override fun getCredentials(): Any = apiKey
    override fun getPrincipal(): Any? = principalValue
}