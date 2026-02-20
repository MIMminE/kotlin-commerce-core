package nuts.commerce.productservice.adapter.security.apikey

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component

@Component
@EnableConfigurationProperties(ApiKeyProperties::class)
class StaticApiKeyAuthenticator(
    private val props: ApiKeyProperties
) : ApiKeyAuthenticator {

    override fun authenticate(apiKey: String): ApiKeyAuthResult? {
        val entry = props.api.firstOrNull { it.key == apiKey } ?: return null

        return ApiKeyAuthResult(
            principal = entry.principal,
            roles = listOf(entry.role)
        )
    }
}

@ConfigurationProperties(prefix = "security")
data class ApiKeyProperties(
    val api: List<ApiEntry> = emptyList()
) {
    data class ApiEntry(
        val key: String,
        val role: String,
        val principal: String
    )
}
