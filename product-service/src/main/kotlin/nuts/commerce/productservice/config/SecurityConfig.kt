package nuts.commerce.productservice.config

import nuts.commerce.productservice.adapter.security.filter.JwtOrApiKeyAuthenticationFilter
import nuts.commerce.productservice.adapter.security.provider.ApiKeyAuthenticationProvider
import nuts.commerce.productservice.adapter.security.provider.JwtAuthenticationProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun authenticationManager(
        jwtAuthenticationProvider: JwtAuthenticationProvider,
        apiKeyAuthenticationProvider: ApiKeyAuthenticationProvider
    ): AuthenticationManager {
        return ProviderManager(listOf(jwtAuthenticationProvider, apiKeyAuthenticationProvider))
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity, authenticationManager: AuthenticationManager): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/api/products/search/**").hasAnyRole("USER", "DEV", "PROD", "ADMIN")
                auth.requestMatchers("/api/products").hasAnyRole("DEV", "PROD", "ADMIN")
                    .anyRequest().permitAll()
            }
            .addFilterBefore(
                JwtOrApiKeyAuthenticationFilter(authenticationManager),
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }
}