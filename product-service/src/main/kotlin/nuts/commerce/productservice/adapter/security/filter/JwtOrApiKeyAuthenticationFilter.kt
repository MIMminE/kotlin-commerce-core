package nuts.commerce.productservice.adapter.security.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import nuts.commerce.productservice.adapter.security.token.ApiKeyAuthenticationToken
import nuts.commerce.productservice.adapter.security.token.JwtAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtOrApiKeyAuthenticationFilter(
    private val authenticationManager: AuthenticationManager
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (SecurityContextHolder.getContext().authentication?.isAuthenticated == true) {
            filterChain.doFilter(request, response)
            return
        }

        val bearer = request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substringAfter("Bearer ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        val apiKey = request.getHeader("X-API-Key")
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (bearer == null && apiKey == null) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val authRequest: Authentication =
                when {
                    bearer != null -> JwtAuthenticationToken.unauthenticated(bearer)
                    else -> ApiKeyAuthenticationToken.unauthenticated(apiKey!!)
                }

            val authResult = authenticationManager.authenticate(authRequest)

            val context = SecurityContextHolder.createEmptyContext()
            context.authentication = authResult
            SecurityContextHolder.setContext(context)

            filterChain.doFilter(request, response)
        } catch (ex: Exception) {
            SecurityContextHolder.clearContext()
            response.status = HttpServletResponse.SC_UNAUTHORIZED
        }
    }
}