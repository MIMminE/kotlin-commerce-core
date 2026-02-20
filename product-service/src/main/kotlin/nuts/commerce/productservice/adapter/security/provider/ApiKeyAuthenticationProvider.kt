package nuts.commerce.productservice.adapter.security.provider

import nuts.commerce.productservice.adapter.security.apikey.ApiKeyAuthenticator
import nuts.commerce.productservice.adapter.security.token.ApiKeyAuthenticationToken
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component

@Component
class ApiKeyAuthenticationProvider(
    private val apiKeyAuthenticator: ApiKeyAuthenticator
) : AuthenticationProvider {
    override fun authenticate(authentication: Authentication): Authentication? {
        val token = authentication as ApiKeyAuthenticationToken

        val apiKey = token.apiKey.trim()
        if (apiKey.isBlank()) throw BadCredentialsException("Missing API key")

        val result = apiKeyAuthenticator.authenticate(apiKey)
            ?: throw BadCredentialsException("Invalid API key")

        val authorities = result.roles.map { SimpleGrantedAuthority("ROLE_$it") }

        return ApiKeyAuthenticationToken.authenticated(
            apiKey = apiKey,
            principal = result.principal,
            authorities = authorities
        )
    }

    override fun supports(authentication: Class<*>): Boolean {
        return ApiKeyAuthenticationToken::class.java.isAssignableFrom(authentication)
    }
}