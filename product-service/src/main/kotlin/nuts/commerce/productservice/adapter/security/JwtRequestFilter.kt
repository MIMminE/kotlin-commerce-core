package nuts.commerce.productservice.adapter.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtRequestFilter(
    private val jwtComponent: JwtComponent,
) :
    OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authorizationHeader = request.getHeader("Authorization")

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            val jwtToken = authorizationHeader.substring(7)

            if (jwtComponent.validateToken(jwtToken)) {
                val username = jwtComponent.getUsernameFromToken(jwtToken)!!
                val roles = jwtComponent.getRoleFromToken(jwtToken)

                val authentication = UsernamePasswordAuthenticationToken(
                    username, null, roles.map { SimpleGrantedAuthority(it) }
                )

                SecurityContextHolder.getContext().authentication = authentication
            }
        }

        filterChain.doFilter(request, response)
    }
}